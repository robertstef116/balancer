import React from 'react';
import WidgetWrapper from "./WidgetWrapper";

const TableWidget = () => {

  const onAddClick = () => {
    console.log('addd')
  }

  return (
    <WidgetWrapper title='Workers List' actions={[
      {title: 'Add worker', icon: 'bi-plus', onClick: onAddClick}
    ]}>
      <div style={{width: '300px', height: '200px'}}>

      </div>
    </WidgetWrapper>
  );
};

export default TableWidget;
