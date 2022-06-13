import React from 'react';
import classNames from 'classnames';
import Spinner from '../components/Spinner';
import { Icons } from '../constants';

function WidgetWrapper({
  className, children, title, isLoading, customAction, error = null, dismissError = null, onRefresh = null, actions = [],
}) {
  return (
    <div className={`p-4 animate__animated animate__zoomIn animate__faster ${className}`}>
      <span className="d-flex align-items-center widget-header">
        <div className="fw-bold d-flex align-items-center text-dark">{title}</div>
        <span className="ms-auto d-flex align-items-center">
          {onRefresh && (
          <i
            className={classNames('bi ps-1 widget-action', Icons.REFRESH, {
              invisible: isLoading,
            })}
            title="Refresh"
            onClick={onRefresh}
          />
          )}
          {actions.map((action) => (
            <i
              key={action.icon}
              className={classNames(`bi ps-1 widget-action ${action.icon}`, {
                invisible: isLoading || action.hide,
              })}
              title={action.title}
              onClick={action.onClick}
            />
          ))}
          {customAction}
        </span>
      </span>
      <div className={classNames(
        'd-flex flex-column widget shadow border border-primary position-relative rounded w-100 h-100 bg-light',
        { 'border-danger': !!error },
      )}
      >
        {error && (
        <div className="bg-danger text-light px-1 d-inline-flex justify-content-between rounded m-1 text-truncate">
          <span className="text-truncate" title={error}>
            {error}
          </span>
          <i className="icon-button bi bi-x-lg" onClick={dismissError} />
        </div>
        )}
        <div className="flex-grow-1 overflow-auto">
          {children}
        </div>
        <Spinner visible={isLoading} />
      </div>
    </div>
  );
}

export default WidgetWrapper;
