import * as types from './types';
import { Icons, WorkerNodeStatus } from '../constants';

const INITIAL_STATE = {
  isAuthenticated: false,
  username: '',
  token: null,

  workers: null,
  workflows: null,
  deployments: null,
  configs: null,
};

const getWorkerNodeStatusIcon = (status) => {
  switch (status) {
    case WorkerNodeStatus.STARTED:
      return Icons.NODE_STARTED;
    case WorkerNodeStatus.STARTING:
      return Icons.NODE_STARTING;
    case WorkerNodeStatus.STOPPED:
      return Icons.NODE_STOPPED;
    case WorkerNodeStatus.STOPPING:
      return Icons.NODE_STOPPING;
    default:
      return '';
  }
};

const createWorkerData = (worker) => ({
  id: worker.id,
  status: worker.status,
  statusIcon: getWorkerNodeStatusIcon(worker.status),
  statusTitle: worker.status.charAt(0).toUpperCase() + worker.status.slice(1).toLowerCase(),
  alias: worker.alias,
  host: worker.host,
  port: worker.port,
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
    minDeployments: workflow.minDeployments,
    maxDeployments: workflow.maxDeployments,
    upScaling: workflow.upScaling,
    downScaling: workflow.downScaling,
    algorithm: workflow.algorithm,
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
      updWorkflow.upScaling = payload.upScaling;
      updWorkflow.downScaling = payload.downScaling;
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
    case types.ADD_WORKER:
      return {
        ...state,
        workers: [
          ...state.workers,
          createWorkerData(payload.worker),
        ],
      };
    case types.UPDATE_WORKER:
      const updWorker = state.workers.find((w) => w.id === payload.id);
      updWorker.alias = payload.alias;
      updWorker.port = payload.port;
      return {
        ...state,
        workers: [
          ...state.workers,
        ],
      };
    case types.DISABLE_WORKER:
      const disWorker = state.workers.find((w) => w.id === payload.id);
      disWorker.status = disWorker.status === WorkerNodeStatus.STARTED || disWorker.status === WorkerNodeStatus.STARTING
        ? WorkerNodeStatus.STOPPING
        : WorkerNodeStatus.STARTING;
      disWorker.statusIcon = getWorkerNodeStatusIcon(disWorker.status);
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
    case types.GET_DEPLOYMENTS:
      const { deployments } = payload;
      return {
        ...state,
        deployments: deployments.map((deployment) => ({
          id: deployment.id,
          workerId: deployment.workerId,
          workflowId: deployment.workflowId,
          containerId: deployment.containerId,
          timestamp: deployment.timestamp,
        })),
      };
    case types.SAVE_CONFIGS:
      const { configs } = payload;
      return {
        ...state,
        configs: {
          ...state.configs,
          ...configs,
        },
      };
    case types.GET_CONFIGS:
    case types.SET_CREDENTIALS:
    case types.REMOVE_CREDENTIALS:
    case types.LOGOUT:
    case types.LOGIN:
    case types.SESSION_EXPIRED:
      return {
        ...state,
        ...payload,
      };
    default:
      return INITIAL_STATE;
  }
};
