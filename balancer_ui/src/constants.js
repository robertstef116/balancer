export const APP_NAME = 'Balancing';
export const API_URL = process.env.REACT_APP_API_URL || `${window.location.origin}/api/v1`;

export const ranges = [
  { text: 'Last 10 min', value: 10, unit: 'm' },
  { text: 'Last 30 min', value: 30, unit: 'm' },
  { text: 'Last hour', value: 60, unit: 'm' },
  { text: 'Last 3 hour', value: 3 * 60, unit: 'm' },
  { text: 'Last 6 hour', value: 6 * 60, unit: 'h' },
  { text: 'Last 12 hour', value: 12 * 60, unit: 'h' },
  { text: 'Last day', value: 24 * 60, unit: 'h' },
  { text: 'Last week', value: 7 * 24 * 60, unit: 'd' },
  { text: 'Last month', value: 30 * 24 * 60, unit: 'd' },
];

export const Icons = {
  ADD: 'bi bi-plus-lg',
  EDIT: 'bi bi-pencil',
  DELETE: 'bi bi-x-lg',
  FREEZE: 'bi bi-snow',
  START: 'bi bi-play-circle',
  REFRESH: 'bi bi-arrow-clockwise',
  ARROW_RIGHT: 'bi bi-arrow-right',
  NODE_ONLINE: 'bi bi-cloud-arrow-up text-success',
  NODE_OFFLINE: 'bi bi-cloud-arrow-down text-danger',
  NODE_DISABLED: 'bi bi-stop-circle text-info',
  SAVE: 'bi bi-check-lg',
  ERROR: 'bi bi-x-circle-fill',
  INFO: 'bi bi-info-circle',
};

export const WorkerNodeStatus = {
  ONLINE: 'ONLINE',
  OFFLINE: 'OFFLINE',
  DISABLED: 'DISABLED',
};

export const algorithms = [
  { display: 'Random', value: 'RANDOM' },
  { display: 'Least connection', value: 'LEAST_CONNECTION' },
  { display: 'Round robin', value: 'ROUND_ROBIN' },
  { display: 'Weighted response time', value: 'WEIGHTED_RESPONSE_TIME' },
  { display: 'Weighted score', value: 'WEIGHTED_SCORE' },
  { display: 'Adaptive', value: 'ADAPTIVE' },
];

export const ModalFormModes = {
  ADD: 'ADD',
  UPDATE: 'UPDATE',
};

export const defaultRange = ranges[6];

export const GetLineColor = (key) => {
  const colors = ['#e51c23', '#e91e63', '#9c27b0', '#673ab7', '#3f51b5', '#5677fc', '#03a9f4', '#00bcd4', '#009688', '#259b24', '#8bc34a', '#afb42b', '#ff9800', '#ff5722', '#795548', '#607d8b'];

  let hash = 0;
  if (key.length === 0) return hash;
  for (let i = 0; i < key.length; i++) {
    // eslint-disable-next-line no-bitwise
    hash = key.charCodeAt(i) + ((hash << 5) - hash);
    // eslint-disable-next-line no-bitwise
    hash &= hash;
  }
  hash = ((hash % colors.length) + colors.length) % colors.length;
  return colors[hash];
};
