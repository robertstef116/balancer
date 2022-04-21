import * as types from './types'

const INITIAL_STATE = {
  isAuthenticated: false,
  username: '',
  token: null,
};

export default (state = INITIAL_STATE, {type, payload}) => {
  switch (type) {
    case types.SET_CREDENTIALS:
    case types.REMOVE_CREDENTIALS:
    case types.LOGOUT:
    case types.LOGIN:
      return {
        ...state,
        ...payload
      };
    default:
      return INITIAL_STATE;
  }
}
