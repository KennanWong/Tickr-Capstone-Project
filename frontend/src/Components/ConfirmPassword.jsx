import React from 'react'

import {CentredBox, H3  } from '../Styles/HelperStyles'
import Alert from '@mui/material/Alert';
import Collapse from '@mui/material/Collapse';
import { Box } from '@mui/system';
import {CircularProgress,Divider, OutlinedInput, Typography } from '@mui/material';
import PasswordInput from "../Components/PasswordInput";

import { FormInput, TextButton, TkrButton } from '../Styles/InputStyles';
import Backdrop from '@mui/material/Backdrop';
import { apiFetch, getToken, setFieldInState, setToken } from '../Helpers';
import { useNavigate } from 'react-router-dom';

export default function ConfirmPassword ({open, handleOpen, route, method, navigateTo}) {
  const navigate = useNavigate()
  const [loading, setLoading] = React.useState(false)

  const [password, setPassword] = React.useState({
    password: '',
    error: false,
    errorMsg: ''
  })

  const [error, setError] = React.useState(false)
  const [errorMsg, setErrorMsg] = React.useState(false)

  const onSubmit = async (e) => {
    setLoading(true)
    try {
      const body = {
        auth_token: getToken(),
        password: password.password
      }
      const response = await apiFetch(method, route, body)
      setToken(null)
      navigate(navigateTo)
      window.location.reload(false);
    } catch (e) {
      setError(true)
      setErrorMsg(e.reason)
      setFieldInState('error', true, password, setPassword)
      setLoading(false)
    }
  }

  React.useEffect(() => {
    setLoading(false)
  }, [open])

  return (
    <Backdrop
      sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
      open={open}
    >
      <Box sx={{width: 400, backgroundColor: "#FFFFFF", borderRadius: 2}}>
        <H3
          sx={{
            fontSize: '30px',
            color: 'black'
          }}
        >
          Confirm Password
        </H3>
        <Divider/>
        <br/>
        <CentredBox>
          <PasswordInput state={password} setState={setPassword} setError={setError} placeholder="Password"/>
        </CentredBox>
        <br/>
        <FormInput>
          <CentredBox sx={{position: 'relative'}}>
            <TkrButton disabled={loading || (password.password === '')} variant="contained" onClick={onSubmit}>Proceed</TkrButton>
            {loading && (
              <CircularProgress 
                size={24}
                sx={{
                  color: "#AE759F",
                  position: 'absolute',
                  top: '50%',
                  left: '50%',
                  marginTop: '-12px',
                  marginLeft: '-12px',
                }}
              />
            )}
          </CentredBox>
          <Collapse in={error}>
            <Alert severity="error">{errorMsg}</Alert>
          </Collapse>
        </FormInput>
        <br/>
        <CentredBox>
          <TextButton onClick={handleOpen}>
            Cancel
          </TextButton>
        </CentredBox>
        <br/>
      </Box>
    </Backdrop>
  )
}