const labels: Record<string, string> = {
  submitted: 'Đã tiếp nhận', start: 'Đã tiếp nhận',
  triaged: 'Đã phân loại', triage: 'Đã phân loại',
  assigned: 'Đang xử lý', assign: 'Đã giao xử lý',
  resolved: 'Đã giải quyết', resolve: 'Đã giải quyết',
};
export function reportState(value: string) { return labels[value] || value || 'Đã tiếp nhận'; }
