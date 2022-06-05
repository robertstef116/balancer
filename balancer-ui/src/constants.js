export const APP_NAME = 'Balancing';
export const API_URL = 'http://localhost:8010';

export const ranges = [
  { text: 'Last 10 min', value: 10 * 60, unit: 'm' },
  { text: 'Last 30 min', value: 30 * 60, unit: 'm' },
  { text: 'Last hour', value: 60 * 60, unit: 'm' },
  { text: 'Last 3 hour', value: 3 * 60 * 60, unit: 'm' },
  { text: 'Last 6 hour', value: 6 * 60 * 60, unit: 'h' },
  { text: 'Last 12 hour', value: 12 * 60 * 60, unit: 'h' },
  { text: 'Last day', value: 24 * 60 * 60, unit: 'h' },
  { text: 'Last week', value: 7 * 24 * 60 * 60, unit: 'd' },
  { text: 'Last month', value: 30 * 24 * 60 * 60, unit: 'w' },
];

export const defaultRange = ranges[2];
