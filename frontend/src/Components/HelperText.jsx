import React from 'react'
import FormHelperText from '@mui/material/FormHelperText';
import FormControl, { useFormControl } from '@mui/material/FormControl';

export default function HelperText({state, field}) {
  const { focused } = useFormControl() || {};

  const helperText = React.useMemo(() => {
    if (focused) {
      return state[field];
    }
    return '';
  }, [focused]);
  
  return <FormHelperText>{helperText}</FormHelperText>;
}