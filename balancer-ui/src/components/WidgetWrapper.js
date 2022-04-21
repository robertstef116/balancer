import React from 'react';

const WidgetWrapper = ({children, title, actions = []}) => {

  return (
    <div className='d-inline-block m-5 animate__animated animate__zoomIn animate__faster'>
      <span className='d-flex widget-header'>
        <div className='fw-bold d-flex align-items-center text-dark'>{title}</div>
        <span className='ms-auto'>
          {actions.map(action =>
            <i className={'bi ps-1 widget-action ' + action.icon} title={action.title} onClick={action.onClick}/>
          )}
        </span>
      </span>
      <div className='widget shadow border border-primary rounded'>
        {children}
      </div>
    </div>
  );
};

export default WidgetWrapper;
