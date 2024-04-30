import React from "react";

import Box from '@mui/material/Box';
import Divider from '@mui/material/Divider';
import FormControl, { useFormControl } from '@mui/material/FormControl';
import FormHelperText from '@mui/material/FormHelperText';
import Grid from '@mui/material/Unstable_Grid2';
import Alert from '@mui/material/Alert';
import Collapse from '@mui/material/Collapse';
import FormLabel from '@mui/material/FormLabel';
import {CircularProgress, IconButton, Tooltip} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { FormInput, TkrButton} from '../Styles/InputStyles';
import '../App.css';
import { apiFetch, getToken, loggedIn, passwordCheck, setFieldInState, setToken } from '../Helpers';
import { FlexRow, Logo, H3, CentredBox } from '../Styles/HelperStyles';
import HelperText from '../Components/HelperText';
import { Link, useNavigate, useParams } from "react-router-dom";
import StandardLogo from "../Components/StandardLogo";
import PasswordInput from "../Components/PasswordInput";

export default function ChangePassword({}) {
  const navigate = useNavigate()
  const params = useParams()

  const [currentPW, setCurrentPW] = React.useState({
    password: '',
    error: false,
    errorMsg: '',
    visibility: false,
  })

  const [newPW, setNewPW] = React.useState({
    password: '',
    error: false,
    errorMsg: '',
    visibility: false,
  })
  const [confirmNewPW, setConfirmNewPW] = React.useState({
    password: '',
    error: false,
    errorMsg: '',
    visibility: false,
  })

  const [error, setError] = React.useState(false)
  const [errorMsg, setErrorMsg] = React.useState("")
  const [loading, setLoading] = React.useState(false)

  const PasswordChange = (e) => {
    setFieldInState('password', e.target.value, newPW, setNewPW);
    // Check for digit

    var hasUpper = newPW.password.match(/[A-Z]/);
    var hasDigit = newPW.password.match(/[0-9]/);
    var hasSpecial = newPW.password.match(/[!@#$%^&*]/);
    var hasLength = (newPW.password.length >= 8);

    var validPassword = true;
    var errorMsg = 'Password must contain';

    if (!hasUpper) {
      errorMsg = errorMsg + ' an uppercase character';
      validPassword = false;
    } 
    if (!hasDigit) {
      if (errorMsg !== 'Password must contain') {
        errorMsg = errorMsg + ', a digit';
      } else {
        errorMsg = errorMsg + ' a digit';
      }
      validPassword = false;
    } 
    if (!hasSpecial) {
      if (errorMsg !== 'Password must contain') {
        errorMsg = errorMsg + ', a special character';
      } else {
        errorMsg = errorMsg + ' a special character';
      }
      validPassword = false;
    } 

    if (!hasLength) {
      if (errorMsg !== 'Password must contain') {
        errorMsg = errorMsg + ', 8 characters';
      } else {
        errorMsg = errorMsg + ' 8 characters';
      }
      validPassword = false;
    } 

    setFieldInState('error', !validPassword, newPW, setNewPW)
    if (!validPassword) {
      setFieldInState('errorMsg', errorMsg, newPW, setNewPW)
    }
  }

  function PasswordHelperText() {
    const { focused } = useFormControl() || {};
    const helperText = React.useMemo(() => {
      if (focused) {
        return newPW.errorMsg;
      }
      return '';
    }, [focused]);
    
    return <FormHelperText>{helperText}</FormHelperText>;
  }

  React.useEffect(() => {
    console.log(error)
    if (!error) {
      setFieldInState('error', false, newPW, setNewPW)
      setFieldInState('error', false, confirmNewPW, setConfirmNewPW)
    }
  }, [error])

  const handleSubmit = async (e) => {
    // Set loading until request completes'

    setLoading(true)
    
    // Check form input
    var validForm = true
    if (loggedIn() && currentPW.password.length <= 0) {
      setLoading(false)
      setError(true)
      setErrorMsg("Please fill in required fields")
      setFieldInState('error', true, currentPW, setCurrentPW)
      validForm = false
    } 
    if (newPW.password.length <= 0) {
      setLoading(false)
      setError(true)
      setErrorMsg("Please fill in required fields")
      setFieldInState('error', true, newPW, setNewPW)
      validForm = false
    } 
    
    if (confirmNewPW.password.length <= 0) {
      setLoading(false)
      setError(true)
      setErrorMsg("Please fill in required fields")
      setFieldInState('error', true, confirmNewPW, setConfirmNewPW)
      validForm = false
    } 

    if (confirmNewPW.password !== newPW.password) {
      setLoading(false)
      setError(true)
      setErrorMsg("Passwords do not match")
      setFieldInState('error', true, newPW, setNewPW)
      setFieldInState('error', true, confirmNewPW, setConfirmNewPW)
      validForm = false
    }

    if (!validForm) {
      return
    }

    // Check if password is a valid password
    if (!passwordCheck(newPW.password)) {
      setLoading(false)
      setError(true)
      setErrorMsg("Enter a valid password")
      setFieldInState('error', true, newPW, setNewPW)
      return
    }

    // Check passwords match, raise an error otherwise
    if (newPW.password !== confirmNewPW.password) {
      setLoading(false)
      setError(true)
      setErrorMsg("Passwords do not match")
      setFieldInState('error', true, newPW, setNewPW)
      setFieldInState('error', true, confirmNewPW, setConfirmNewPW)
      return
    }

    if ((newPW.password === currentPW.password) && loggedIn()) {
      setLoading(false)
      setError(true)
      setErrorMsg("New password can not be the old password.")
      setFieldInState('error', true, currentPW, setCurrentPW)
      setFieldInState('error', true, newPW, setNewPW)
      setFieldInState('error', true, confirmNewPW, setConfirmNewPW)
      return
    }

    // Send api call
    try {
      // Split if a reset token is required
      if (loggedIn()) {
        const body = {
          password: currentPW.password,
          new_password: newPW.password,
          auth_token: getToken()
        }
        try {
          const response = await apiFetch('PUT', '/api/user/reset', body)
          setToken(response.auth_token)
          navigate("/my_profile")
        } catch (error) {
          setLoading(false)
          setError(true)
          setErrorMsg("Current password does not match records.")
          setFieldInState('error', true, currentPW, setCurrentPW)
        }
        // navigate("/")
      } else {
        const resetToken = params.resetToken
        const email = params.email
        const body = {
          reset_token: resetToken,
          email: email,
          new_password: newPW.password
        }
        try {
          const response = await apiFetch('PUT', '/api/user/reset/complete', body)
          navigate("/login")
        } catch (e) {
          setLoading(false)
          setError(true)
          setErrorMsg(e.reason)
          setFieldInState('error', true, currentPW, setCurrentPW)
        }
      }
    } catch (e) {
      console.log(e)
    }
  }

  return (
    <div>
      <StandardLogo/>
      <Box disabled className='Background'> 
        <Box disabled sx={{ boxShadow: 3, width:500, borderRadius:2, backgroundColor: '#F5F5F5', padding: 1}} >
          <Grid container spacing={2}>
            <Grid item xs={1}>
              {(loggedIn())
                ? <CentredBox sx={{height: '100%', pl: 5, alignItems: 'center'}}>
                    <Tooltip title="Back to profile">
                      <IconButton onClick={() => {navigate('/my_profile')}}>
                        <ArrowBackIcon/>
                      </IconButton>
                    </Tooltip>
                  </CentredBox>
                : <></>
              }
            </Grid>
            <Grid item xs={10}>
              <H3
                sx={{
                  fontSize: '30px',
                }}
              >
                Change Password
              </H3>
            </Grid>
            <Grid item xs={1}>

            </Grid>
          </Grid>
          
          <Divider/>
          <br/>
          <FormInput>
            <Grid container spacing={2} sx={{paddingLeft: 5, paddingRight: 5}}>
              {(loggedIn())
                ? <Grid xs={12}>
                    <FormControl fullWidth={true}>
                      <FormLabel sx={{"&.Mui-focused": {color: 'rgba(0, 0, 0, 0.6) '}}}>Confirm current password</FormLabel>
                      <PasswordInput state={currentPW} setState={setCurrentPW} setError={setError} placeholder="Current Password"/>
                    </FormControl>
                  </Grid>
                : <div></div>
              }
              <Grid xs={12}>
                <FormControl fullWidth={true}>
                  <FormLabel sx={{"&.Mui-focused": {color: 'rgba(0, 0, 0, 0.6) '}}}>New Password</FormLabel>
                  <PasswordInput state={newPW} setState={setNewPW} setError={setError} placeholder="New Password" requirements={true}/>
                  <PasswordHelperText/>
                </FormControl>
              </Grid>
              <Grid xs={12}>
                <FormControl fullWidth={true}>
                  <FormLabel sx={{"&.Mui-focused": {color: 'rgba(0, 0, 0, 0.6) '}}}>Confirm new password</FormLabel>
                  <PasswordInput state={confirmNewPW} setState={setConfirmNewPW} setError={setError} placeholder="Confirm Password"/>
                </FormControl>
              </Grid>
            </Grid>
          </FormInput>
          <br/>
          <Divider/>
          <br/>
          <FormInput>
            <CentredBox sx={{position: 'relative'}}>
              <TkrButton
                variant="contained"
                disabled={loading}
                sx={{fontSize: 17}}
                onClick={handleSubmit}
              >
                Change Password
              </TkrButton>
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
        </Box>
      </Box>
    </div>
  )
}