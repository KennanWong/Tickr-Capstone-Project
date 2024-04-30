import React from  'react'
import { setFieldInState } from '../Helpers'
import { ContrastInput, ContrastInputWrapper } from '../Styles/InputStyles'

export default function ShadowInput({backgroundColor='#FFFFFF', state, field, setState, sx={}, defaultValue='', placeholder='', setError=null, type='text'}) {

  const onChange = (e) => {
    setFieldInState(field, e.target.value, state, setState)
    if (Object.keys(state).includes('error')) {
      setFieldInState('error', false, state, setState)
    }
    if (setError !== null) {
      setError(false)
    }
  }

  return (
    <ContrastInputWrapper>
      <ContrastInput type={type} value={state[field]} fullWidth sx={sx} onChange={onChange} placeholder={placeholder}>
      </ContrastInput>
    </ContrastInputWrapper>
  )
}