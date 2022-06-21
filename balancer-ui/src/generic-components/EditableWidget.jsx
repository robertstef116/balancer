import React from 'react';
import WidgetWrapper from './WidgetWrapper';
import { Icons } from '../constants';

function EditableWidget({
  title, onSave, changed, children, className, widgetProps,
}) {
  return (
    <WidgetWrapper
      title={title}
      className={className}
      widgetProps={widgetProps}
      actions={[
        {
          title: 'Save', icon: Icons.SAVE, onClick: onSave, hide: !changed,
        },
      ]}
    >
      {children}
    </WidgetWrapper>
  );
}

export default EditableWidget;
