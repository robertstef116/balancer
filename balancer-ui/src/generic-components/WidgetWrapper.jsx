import React from 'react';
import classNames from 'classnames';
import Spinner from './Spinner';

function WidgetWrapper({
  className, children, title, isLoading, error = null, dismissError = null, actions = [],
}) {
  return (
    <div className={`p-4 animate__animated animate__zoomIn animate__faster ${className}`}>
      <span className="d-flex">
        <div className="fw-bold d-flex align-items-center text-dark">{title}</div>
        <span className="ms-auto">
          {actions.map((action) => (
            <i
              key={action.icon}
              className={`bi ps-1 widget-action ${action.icon}${isLoading || action.hide ? ' invisible' : ''}`}
              title={action.title}
              onClick={action.onClick}
            />
          ))}
        </span>
      </span>
      <div className={classNames(
        'd-flex flex-column widget shadow border border-primary rounded w-100 h-100 bg-light',
        { 'border-danger': !!error },
      )}
      >
        {error && (
        <div className="bg-danger text-light px-1 d-inline-flex justify-content-between rounded m-1">
          {error}
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
