import thunk from 'redux-thunk';
import { applyMiddleware, createStore } from 'redux';
import Reducer from './reducers';

const initialState = {};

// const reducers = combineReducers({
//   loginReducer: Reducer,
// });

const middleware = [thunk];

const store = createStore(
  Reducer,
  initialState,
  applyMiddleware(...middleware),
);

export default store;
