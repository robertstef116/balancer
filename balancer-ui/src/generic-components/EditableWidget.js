import React from 'react';
import WidgetWrapper from "./WidgetWrapper";

const EditableWidget = ({title, isLoading, onSave, changed, children, error, dismissError, className}) => {
  return <WidgetWrapper title={title} isLoading={isLoading} className={className} error={error} dismissError={dismissError}
                        actions={[
                          {title: 'Save', icon: 'bi-check', onClick: onSave, hide: !changed}
                        ]}>
    {children}
  </WidgetWrapper>
};

export default EditableWidget;
