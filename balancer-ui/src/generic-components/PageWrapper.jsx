import React from 'react';
import classNames from 'classnames';
import Spinner from '../components/Spinner';
import { Icons } from '../constants';

function PageWrapper({
  children, isLoading, error = null, dismissError = null, onRefresh = null,
}) {
  return (
    <>
      <div className="d-flex page-header">
        {error && (
          <div className="bg-danger page-error text-light px-1 d-inline-flex justify-content-between text-truncate">
            <span className="text-truncate" title={error}>
              {error}
            </span>
            <i className={classNames('icon-button ms-2 me-1', Icons.DELETE)} onClick={dismissError} />
          </div>
        )}
        {onRefresh && (
          <div className="bg-dark ms-auto ps-5 pe-3 page-action-wrapper position-relative">
            <i
              className={classNames('ps-1 page-action', Icons.REFRESH, {
                invisible: isLoading,
              })}
              title="Refresh"
              onClick={onRefresh}
            />
            <Spinner visible={isLoading} small transparent />
          </div>
        )}
      </div>
      <div className="mx-0 mx-md-1 mx-xl-3 mx-xxl-5">
        <div className="container-fluid">
          {children}
        </div>
      </div>
    </>
  );
}

export default PageWrapper;
