import * as types from './types';
import { Icons, WorkerNodeStatus } from '../constants';

const INITIAL_STATE = {
  isAuthenticated: false,
  username: '',
  token: null,

  workers: null,
  workflows: null,
};

const getWorkerNodeStateIcon = (status) => {
  switch (status) {
    case WorkerNodeStatus.ONLINE:
      return Icons.NODE_ONLINE;
    case WorkerNodeStatus.OFFLINE:
      return Icons.NODE_OFFLINE;
    case WorkerNodeStatus.DISABLED:
      return Icons.NODE_DISABLED;
    default:
      return '';
  }
};

const createWorkerData = (worker) => ({
  id: worker.id,
  state: worker.state,
  stateIcon: getWorkerNodeStateIcon(worker.state),
  stateTitle: worker.state.charAt(0).toUpperCase() + worker.state.slice(1).toLowerCase(),
  alias: worker.alias,
});

const getAlgorithmDisplay = (alg) => {
  const algorithm = alg.replaceAll('_', ' ').toLowerCase();
  return algorithm.charAt(0).toUpperCase() + algorithm.slice(1);
};

const createWorkflowData = (workflow) => {
  let mapping = '';
  Object.keys(workflow.pathsMapping).forEach((path) => {
    mapping += `${path} - ${workflow.pathsMapping[path]}\n`;
  });
  return {
    id: workflow.id,
    image: workflow.image,
    workerId: workflow.workerId,
    memoryLimit: workflow.memoryLimit,
    cpuLimit: workflow.cpuLimit,
    minDeployments: workflow.minDeployments,
    maxDeployments: workflow.maxDeployments,
    algorithm: workflow.algorithm,
    pathsMapping: workflow.pathsMapping,
    algorithmDisplay: getAlgorithmDisplay(workflow.algorithm),
    mapping,
  };
};

// eslint-disable-next-line default-param-last
export default (state = INITIAL_STATE, { type, payload }) => {
  switch (type) {
    case types.GET_WORKFLOWS:
      const { workflows } = payload;
      return {
        ...state,
        workflows: workflows.map(createWorkflowData),
      };
    case types.ADD_WORKFLOW:
      return {
        ...state,
        workflows: [
          ...state.workflows,
          createWorkflowData(payload.workflow),
        ],
      };
    case types.UPDATE_WORKFLOW:
      const updWorkflow = state.workflows.find((w) => w.id === payload.id);
      updWorkflow.algorithm = payload.algorithm;
      updWorkflow.algorithmDisplay = getAlgorithmDisplay(payload.algorithm);
      updWorkflow.minDeployments = payload.minDeployments;
      updWorkflow.maxDeployments = payload.maxDeployments;
      return {
        ...state,
        workflows: [
          ...state.workflows,
        ],
      };
    case types.DELETE_WORKFLOW:
      const newWorkflows = state.workflows.filter((w) => w.id !== payload.id);
      return {
        ...state,
        workflows: newWorkflows,
      };
    case types.GET_WORKERS:
      const { workers } = payload;
      return {
        ...state,
        workers: workers.map(createWorkerData),
      };
    case types.UPDATE_WORKER:
      const updWorker = state.workers.find((w) => w.id === payload.id);
      updWorker.alias = payload.alias || updWorker.alias;
      updWorker.state = payload.state || updWorker.state;
      updWorker.stateIcon = getWorkerNodeStateIcon(updWorker.state);
      updWorker.stateTitle = updWorker.state.charAt(0).toUpperCase() + updWorker.state.slice(1).toLowerCase();
      return {
        ...state,
        workers: [
          ...state.workers,
        ],
      };
    case types.DELETE_WORKER:
      const newWorkers = state.workers.filter((w) => w.id !== payload.id);
      return {
        ...state,
        workers: newWorkers,
      };
    case types.SET_CREDENTIALS:
    case types.REMOVE_CREDENTIALS:
    case types.LOGOUT:
    case types.LOGIN:
    case types.SESSION_EXPIRED:
      return {
        ...INITIAL_STATE,
        ...payload,
      };
    default:
      return INITIAL_STATE;
  }
};
