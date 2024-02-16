export const APP_NAME = 'Balancing';
export const API_URL = process.env.REACT_APP_API_URL || `${window.location.origin}/api/v1`;

console.log(process.env);

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

export const Icons = {
  ADD: 'bi bi-plus-lg',
  EDIT: 'bi bi-pencil',
  DELETE: 'bi bi-x-lg',
  FREEZE: 'bi bi-snow',
  REFRESH: 'bi bi-arrow-clockwise',
  ARROW_RIGHT: 'bi bi-arrow-right',
  NODE_STARTED: 'bi bi-play-circle text-success',
  NODE_STARTING: 'bi bi-cloud-arrow-up text-info',
  NODE_STOPPED: 'bi bi-stop-circle text-danger',
  NODE_STOPPING: 'bi bi-cloud-arrow-down text-info',
  SAVE: 'bi bi-check-lg',
  ERROR: 'bi bi-x-circle-fill',
  INFO: 'bi bi-info-circle',
};

export const WorkerNodeStatus = {
  STARTED: 'STARTED',
  STARTING: 'STARTING',
  STOPPING: 'STOPPING',
  STOPPED: 'STOPPED',
};

export const algorithms = [
  { display: 'Random', value: 'RANDOM' },
  { display: 'Least connection', value: 'LEAST_CONNECTION' },
  { display: 'Round robin', value: 'ROUND_ROBIN' },
  { display: 'Weighted response time', value: 'WEIGHTED_RESPONSE_TIME' },
];

export const ModalFormModes = {
  ADD: 'ADD',
  UPDATE: 'UPDATE',
};

export const defaultRange = ranges[2];
