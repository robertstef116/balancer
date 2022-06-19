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

export const Icons = {
  ADD: 'bi-plus-lg',
  EDIT: 'bi-pencil',
  DELETE: 'bi-x-lg',
  FREEZE: 'bi-snow',
  REFRESH: 'bi-arrow-clockwise',
  ARROW_RIGHT: 'bi-arrow-right',
  NODE_STARTED: 'bi-play-circle text-success',
  NODE_STARTING: 'bi-cloud-arrow-up text-info',
  NODE_STOPPED: 'bi-stop-circle text-danger',
  NODE_STOPPING: 'bi-cloud-arrow-down text-info',
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
