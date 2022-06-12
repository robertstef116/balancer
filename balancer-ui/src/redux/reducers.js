import * as types from './types';

const INITIAL_STATE = {
  isAuthenticated: false,
  username: '',
  token: null,

  workers: null,
  workflows: null,
  deployments: null,
  configs: null,
};

const createWorkerData = (worker) => ({
  id: worker.id,
  inUse: worker.inUse,
  inUseIcon: worker.inUse ? 'bi-play-circle text-success' : 'bi-stop-circle text-danger',
  alias: worker.alias,
  host: worker.host,
  port: worker.port,
});

// eslint-disable-next-line default-param-last
export default (state = INITIAL_STATE, { type, payload }) => {
  switch (type) {
    case types.GET_WORKFLOWS:
      const { workflows } = payload;
      return {
        ...state,
        workflows: workflows.map((workflow) => {
          let mapping = '';
          Object.keys(workflow.pathsMapping).forEach((path) => {
            mapping += `${path} - ${workflow.pathsMapping[path]}\n`;
          });
          const algorithm = workflow.algorithm.replaceAll('_', ' ').toLowerCase();
          return {
            id: workflow.id,
            image: workflow.image,
            workerId: workflow.workerId,
            memoryLimit: workflow.memoryLimit,
            minDeployments: workflow.minDeployments,
            maxDeployments: workflow.maxDeployments,
            algorithm: algorithm.charAt(0).toUpperCase() + algorithm.slice(1),
            mapping,
          };
        }),
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
      return {
        ...state,
        ...payload,
      };
    default:
      return INITIAL_STATE;
  }
};
