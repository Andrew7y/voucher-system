import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

const claimSuccessRate  = new Rate('claim_success_rate');
const claimSoldOutRate  = new Rate('claim_sold_out_rate');
const claimErrorRate    = new Rate('claim_error_rate');
const claimLatency      = new Trend('api_claim_duration');
const totalClaimed      = new Counter('total_claimed');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

function toLocalDateTimeString(date) {
    return date.toISOString().slice(0, 19);
}

function secondsUntil(date) {
    return Math.max(Math.ceil((date.getTime() - Date.now()) / 1000), 0);
}

export const options = {
    stages: [
        { duration: '5s',  target: 100 },
        { duration: '15s', target: 500 },
        { duration: '5s',  target: 0   },
    ],
    thresholds: {
        'claim_error_rate': ['rate==0'],
        'http_req_failed': ['rate<1.0'],
        'api_claim_duration': ['p(95)<500', 'p(99)<1000'],
    },
};

export function setup() {
    console.log('[Setup] Creating campaign and voucher rules...');

    const startAt = new Date(Date.now() + 10 * 1000);
    const endAt   = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000);

    const payload = JSON.stringify({
        title:  'Flash Sale Load Test',
        startAt: toLocalDateTimeString(startAt),
        endAt:   toLocalDateTimeString(endAt),
        voucherRules: [
            {
                totalQuota:    50,
                discountType:  'PERCENTAGE',
                discountValue: 20.00,
                maxDiscount:   200.00,
                minOrderVal:   500.00,
                conditions:    null,
            },
            {
                totalQuota:    100,
                discountType:  'FIXED_AMOUNT',
                discountValue: 100.00,
                maxDiscount:   100.00,
                minOrderVal:   300.00,
                conditions:    null,
            },
        ],
    });

    const createRes = http.post(
        `${BASE_URL}/api/v1/admin/campaigns`,
        payload,
        { headers: { 'Content-Type': 'application/json' } },
    );

    if (createRes.status !== 201) {
        throw new Error(`[Setup Failed] Cannot create campaign. Status: ${createRes.status}, Body: ${createRes.body}`);
    }

    const campaign = JSON.parse(createRes.body);
    console.log(`[Setup] Campaign created. ID: ${campaign.id}`);
    console.log('[Setup] Publishing campaign...');

    const publishRes = http.post(
        `${BASE_URL}/api/v1/admin/campaigns/${campaign.id}/publish`,
        null,
        { headers: { 'Content-Type': 'application/json' } },
    );

    if (publishRes.status !== 200) {
        throw new Error(`[Setup Failed] Cannot publish campaign. Status: ${publishRes.status}, Body: ${publishRes.body}`);
    }

    const published = JSON.parse(publishRes.body);
    const ruleIds   = published.voucherRules.map((r) => r.id);

    console.log(`[Setup] Campaign published. ID: ${published.id}, Status: ${published.status}`);
    console.log(`[Setup] Rule IDs: ${ruleIds.join(', ')}`);

    const waitSec = secondsUntil(startAt);
    if (waitSec > 0) {
        console.log(`[Setup] Waiting ${waitSec}s until campaign startAt: ${toLocalDateTimeString(startAt)}`);
        sleep(waitSec);
    }

    console.log('[Setup] Campaign start time reached. Starting load test...');

    const totalQuota = published.voucherRules.reduce((sum, r) => sum + r.totalQuota, 0);

    return {
        campaignId: published.id,
        ruleId1:    ruleIds[0],
        ruleId2:    ruleIds[1],
        totalQuota,
    };
}

export default function (data) {
    const userId = (__VU * 1000000) + __ITER;
    const ruleId = Math.random() < 0.5 ? data.ruleId1 : data.ruleId2;

    group('claim_voucher', () => {
        const res = http.post(
            `${BASE_URL}/api/v1/vouchers/claim`,
            JSON.stringify({ userId, ruleId }),
            { headers: { 'Content-Type': 'application/json' } },
        );

        const isClaimed = check(res, {
            'claim: 200 Got Voucher': (r) => r.status === 200,
        });

        const isSoldOut = check(res, {
            'claim: 400 Sold Out (expected)': (r) => r.status === 400,
        });

        const isError = check(res, {
            'claim: 5xx Server Error (MUST BE 0)': (r) => r.status >= 500,
        });

        claimSuccessRate.add(isClaimed);
        claimSoldOutRate.add(isSoldOut);
        claimErrorRate.add(isError);
        claimLatency.add(res.timings.duration);

        if (isClaimed) {
            totalClaimed.add(1);
        }
    });

    sleep(Math.random() * 0.1);
}

export function teardown(data) {
    console.log('');
    console.log('========================================');
    console.log('[Teardown] Load test completed.');
    console.log(`  Campaign ID  : ${data.campaignId}`);
    console.log(`  Rule IDs     : ${data.ruleId1}, ${data.ruleId2}`);
    console.log(`  Total Quota  : ${data.totalQuota}`);
    console.log('');
    console.log('  PASS Criteria:');
    console.log(`    - total_claimed must equal totalQuota (${data.totalQuota})`);
    console.log('    - claim_error_rate must be 0%');
    console.log('    - p(95) latency must be < 500ms');
    console.log('========================================');
}