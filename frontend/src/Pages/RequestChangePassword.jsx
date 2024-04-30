import React from "react";

import Box from '@mui/material/Box';
import Divider from '@mui/material/Divider';
import OutlinedInput from '@mui/material/OutlinedInput';
import FormControl, { useFormControl } from '@mui/material/FormControl';
import FormHelperText from '@mui/material/FormHelperText';
import Grid from '@mui/material/Unstable_Grid2';
import InputAdornment from '@mui/material/InputAdornment';
import IconButton from '@mui/material/IconButton';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import Alert from '@mui/material/Alert';
import Collapse from '@mui/material/Collapse';
import FormControlLabel from '@mui/material/FormControlLabel';
import FormLabel from '@mui/material/FormLabel';
import { FormInput, TextButton, TkrButton} from '../Styles/InputStyles';
import {CircularProgress, Tooltip, Typography } from '@mui/material';
import '../App.css';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { apiFetch, checkValidEmail, getToken, setFieldInState, setToken } from '../Helpers';
import { FlexRow, Logo, H3, CentredBox } from '../Styles/HelperStyles';
import HelperText from '../Components/HelperText';
import { Link, useNavigate } from "react-router-dom";
import StandardLogo from "../Components/StandardLogo";
import PasswordInput from "../Components/PasswordInput";


export default function RequestChangePassword({}) {
  const navigate = useNavigate()

  const [email, setEmail] = React.useState({
    email: '',
    error: false,
    errorMsg: '',
  })

  const [emailConfirmed, setEmailConfirmed] = React.useState(false)

  const [loading, setLoading] = React.useState(false)

  const [inError, setInError] = React.useState(false)
  const [errorMsg, setErrorMsg] = React.useState('')

  const handleRequest = async (e) => {
    // Wait for response
    setLoading(true)

    // Check if email provided is valid
    if (!checkValidEmail(email.email)) {
      setFieldInState('error', true, email, setEmail)
      setFieldInState('errorMsg', 'Enter a valid email.', email, setEmail)
      setLoading(false)
      return
    }
    try {
      const response = await apiFetch('POST', '/api/user/reset/request', {email: email.email})
      setEmailConfirmed(true)
    } catch (error) {
      setFieldInState('error', true, email, setEmail)
      setInError(true)
      setErrorMsg(error.reason)
      setLoading(false)
    }
  }

  React.useEffect(() => {
    if (!inError) {
      setInError(false)
      setErrorMsg('')
    }
  }, [inError])

  const handleChange = (e) => {
    setFieldInState('email', e.target.value, email, setEmail)
    setInError(false)
    setErrorMsg('')
  }

  return (
    <div>
      <StandardLogo/>
      <Box disabled className='Background'> 
        <Box disabled sx={{ boxShadow: 3, width:500, borderRadius:2, backgroundColor: '#F5F5F5', padding: 1}} >
          {emailConfirmed
            ? <CentredBox>
                <H3>
                  Password reset link has been sent to {email.email}
                </H3>
              </CentredBox>
            : <>
              <Grid container>
                <Grid item xs={1}>
                  <CentredBox sx={{height: '100%', pl: 5}}>
                    <Tooltip title="Rememebered your password?">
                      <IconButton onClick={(e) => {navigate('/login')}}>
                        <ArrowBackIcon/>
                      </IconButton>
                    </Tooltip>
                  </CentredBox>
                </Grid>
                <Grid item xs={10}>
                  <H3
                    sx={{
                      fontSize: '30px',
                    }}
                  >
                    Confirm your email
                  </H3>
                </Grid>
                <Grid item xs={1}>

                </Grid>
              </Grid>
              <Divider/>
              <br/>
              <FormInput>
                <Grid container spacing={2} sx={{paddingLeft: 5, paddingRight: 5, width: '90%'}}>
                  <Grid xs={12}>
                    <FormControl fullWidth={true}>
                      <OutlinedInput
                        id="email"
                        placeholder="Email"
                        variant="outlined"
                        sx={{borderRadius: 2}} 
                        fullWidth={true} 
                        error={email.error}
                        onChange={handleChange}
                      />
                      <FormHelperText>{email.errorMsg}</FormHelperText>
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
                    sx={{fontSize: 15}}
                    onClick={handleRequest}
                  >
                    Request Password Change
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
                <Collapse in={inError}>
                  <Alert severity="error">{errorMsg}</Alert>
                </Collapse>
              </FormInput>
              </>
          }
        </Box>
      </Box>
    </div>
  )
}