import http from 'k6/http';
import { check, sleep } from 'k6';

const api = __ENV.PLATERELAY_API_URL || 'http://localhost:8080';
const identity = __ENV.PLATERELAY_IDENTITY_URL || 'http://localhost:9090';
const clientId = __ENV.PLATERELAY_CLIENT_ID || 'platerelay-cli';

export const options = {
  stages: [
    { duration: '15s', target: 5 },
    { duration: '30s', target: 10 },
    { duration: '15s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<1000'],
  },
};

function token(username, password) {
  const response = http.post(`${identity}/realms/platerelay/protocol/openid-connect/token`, {
    client_id: clientId,
    grant_type: 'password',
    username,
    password,
  });
  check(response, { [`${username} token issued`]: (r) => r.status === 200 });
  return response.json('access_token');
}

export function setup() {
  return {
    donor: token(__ENV.PLATERELAY_DONOR_USER || 'donor-dev', __ENV.PLATERELAY_DONOR_PASSWORD || 'donor-local-2026'),
    recipient: token(__ENV.PLATERELAY_RECIPIENT_USER || 'recipient-dev', __ENV.PLATERELAY_RECIPIENT_PASSWORD || 'recipient-local-2026'),
  };
}

export default function (tokens) {
  const donorHeaders = { Authorization: `Bearer ${tokens.donor}`, 'Content-Type': 'application/json' };
  const recipientHeaders = { Authorization: `Bearer ${tokens.recipient}` };

  const browse = http.get(`${api}/api/v1/donations?page=0&size=20`, { headers: recipientHeaders });
  check(browse, { 'browse donations returns 200': (r) => r.status === 200 });

  const until = new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString();
  const donation = http.post(`${api}/api/v1/donations`, JSON.stringify({
    donorId: 'market-17',
    title: `Load-test produce ${__VU}-${__ITER}`,
    description: 'Load-test lot for local performance checks',
    quantity: 3,
    unit: 'BOX',
    availableUntil: until,
    pickup: { latitude: 12.9716, longitude: 77.5946 },
    dietaryTags: ['VEGAN'],
  }), { headers: donorHeaders });
  check(donation, { 'donation publish returns 201': (r) => r.status === 201 });

  const matches = http.get(`${api}/api/v1/matches/recipients/fridge-east?radiusKm=8`, { headers: recipientHeaders });
  check(matches, { 'recipient matching returns 200': (r) => r.status === 200 });

  if (donation.status === 201) {
    const donationId = donation.json('id');
    const cancelled = http.post(`${api}/api/v1/donations/${donationId}/cancel`, null, { headers: donorHeaders });
    check(cancelled, { 'load-test donation is cancelled after matching': (r) => r.status === 200 });
  }
  sleep(0.2);
}
