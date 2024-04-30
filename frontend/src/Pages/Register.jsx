import React from 'react';
import dayjs from 'dayjs';

import Box from '@mui/material/Box';
import Divider from '@mui/material/Divider';
import TextField from '@mui/material/TextField';
import OutlinedInput from '@mui/material/OutlinedInput';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { DesktopDatePicker } from '@mui/x-date-pickers/DesktopDatePicker';
import FormControl, { useFormControl } from '@mui/material/FormControl';
import FormHelperText from '@mui/material/FormHelperText';
import Grid from '@mui/material/Unstable_Grid2';
import InputAdornment from '@mui/material/InputAdornment';
import IconButton from '@mui/material/IconButton';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import Alert from '@mui/material/Alert';
import Collapse from '@mui/material/Collapse';
import { Link, useNavigate } from "react-router-dom";

import { FormInput, TextButton, TkrButton} from '../Styles/InputStyles';

import '../App.css';

import { apiFetch, setFieldInState, setToken } from '../Helpers';
import { FlexRow, Logo, H3 } from '../Styles/HelperStyles';
import HelperText from '../Components/HelperText';
import StandardLogo from '../Components/StandardLogo';
import { Tooltip } from '@mui/material';


export default function Register ({}) {
  // States
  const [userName, setUserName] = React.useState({
    name: '',
    error: false
  })
  const [firstName, setFirstName] = React.useState({
    name: '',
    error: false
  })
  const [lastName, setLastName] = React.useState({
    name: '',
    error: false
  })

  const [email, setEmail] = React.useState({
    email: '',
    error: false,
    forceError: false,
    errorMsg: ''
  })

  const [password, setPassword] = React.useState({
    password: '',
    error: false,
    errorMsg: '',
    visibility: false
  })

  const [confirmPassword, setConfirmPassword] = React.useState({
    password: '',
    error: false,
    errorMsg: '',
    disabled: true,
    visibility: false
  })

  const [DOB, setDOB] = React.useState(dayjs())

  const [errorStatus, setErrorStatus] = React.useState(false)
  const [errorMsg, setErrorMsg] = React.useState(false)

  const navigate = useNavigate()

  const userNameChange = (e) => {
    setFieldInState('name', e.target.value, userName, setUserName)
    setFieldInState('error', false, userName, setUserName)
    setErrorStatus(false)
  }

  const firstNameChange = (e) => {
    setFieldInState('name', e.target.value, firstName, setFirstName)
    setFieldInState('error', false, firstName, setFirstName)
    setErrorStatus(false)
  }

  const lastNameChange = (e) => {
    setFieldInState('name', e.target.value, lastName, setLastName)
    setFieldInState('error', false, lastName, setLastName)
    setErrorStatus(false)
  }

  var validEmail = /^(([^<>()[\]\.,;:\s@\"]+(\.[^<>()[\]\.,;:\s@\"]+)*)|(\".+\"))@(([^<>()[\]\.,;:\s@\"]+\.)+[^<>()[\]\.,;:\s@\"]{2,})$/i;
  const EmailChange = (e) => {
    setFieldInState('email', e.target.value, email, setEmail);
    setFieldInState('forceError', false, email, setEmail)
    if (!email.email.match(validEmail)) {
      setFieldInState('error', true, email, setEmail);
      setFieldInState('errorMsg', 'Enter a valid email', email, setEmail);
    } else {
      setFieldInState('error', false, email, setEmail);
      setFieldInState('errorMsg', '', email, setEmail);
    }
    setErrorStatus(false)
  };

  function EmailHelperText() {
    const { focused } = useFormControl() || {};
    const helperText = React.useMemo(() => {
      if (focused || email.forceError) {
        return email.errorMsg;
      }
      return '';
    }, [focused]);
    return <FormHelperText>{helperText}</FormHelperText>;
  }


  const PasswordChange = (e) => {
    setFieldInState('password', e.target.value, password, setPassword);
    // Check for digit

    var hasUpper = password.password.match(/[A-Z]/);
    var hasDigit = password.password.match(/[0-9]/);
    var hasSpecial = password.password.match(/[!@#$%^&*]/);
    var hasLength = (password.password.length >= 8);

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

    setFieldInState('error', !validPassword, password, setPassword)
    if (!validPassword) {  
      setFieldInState('errorMsg', errorMsg, password, setPassword)
      setFieldInState('disabled', true, confirmPassword, setConfirmPassword)
    } else {
      setFieldInState('disabled', false, confirmPassword, setConfirmPassword)
      if (confirmPassword.password !== password.password && confirmPassword.password.length > 0) {
        setFieldInState('error', true, password, setPassword)
        setFieldInState('error', true, confirmPassword, setConfirmPassword)
        setFieldInState('errorMsg', 'Passwords do not match', password, setPassword)
        setFieldInState('errorMsg', 'Passwords do not match', confirmPassword, setConfirmPassword)
      } else {
        setFieldInState('error', false, confirmPassword, setConfirmPassword)
        setFieldInState('errorMsg', '', password, setPassword)
        setFieldInState('errorMsg', '', confirmPassword, setConfirmPassword)
      }    
    }
    setErrorStatus(false)
  }

  function PasswordHelperText() {
    const { focused } = useFormControl() || {};
    const helperText = React.useMemo(() => {
      if (focused) {
        return password.errorMsg;
      }
      return '';
    }, [focused]);
    
    return <FormHelperText>{helperText}</FormHelperText>;
  }

  const ConfirmPasswordChange = (e) => {
    setFieldInState('password', e.target.value, confirmPassword, setConfirmPassword);
    if (confirmPassword.password !== password.password) {
      setFieldInState('error', true, password, setPassword)
      setFieldInState('error', true, confirmPassword, setConfirmPassword)
      setFieldInState('errorMsg', 'Passwords do not match', password, setPassword)
      setFieldInState('errorMsg', 'Passwords do not match', confirmPassword, setConfirmPassword)
    } else {
      setFieldInState('error', false, password, setPassword)
      setFieldInState('error', false, confirmPassword, setConfirmPassword)
      setFieldInState('errorMsg', '', password, setPassword)
      setFieldInState('errorMsg', '', confirmPassword, setConfirmPassword)
    }
    setErrorStatus(false)
  }

  const checkFields = () => {
    var error = false
    if (firstName.name.length <= 0) {
      setFieldInState('error', true, firstName, setFirstName)
      error = true
    }
    if (lastName.name.length === 0) {
      setFieldInState('error', true, lastName, setLastName)
      error = true
    }
    if (userName.name.length === 0) {
      setFieldInState('error', true, userName, setUserName)
      error = true
    }
    if (email.email.length === 0) {
      setFieldInState('error', true, email, setEmail)
      error = true
    }
    if (password.password.length === 0 || password.password !== confirmPassword.password) {
      setFieldInState('error', true, password, setPassword)
      setFieldInState('error', true, confirmPassword, setConfirmPassword)
      error = true
    }
    if (error) {
      setErrorStatus(true)
    } else {
      setErrorStatus(false)
    }
    return error
  }

  const SubmitRegister = async (e) => {
    if (checkFields()) {
      return
    } 
    console.log("submit register")

    const body = {
      first_name: firstName.name,
      last_name: lastName.name,
      password: password.password,
      user_name: userName.name,
      email: email.email,
      date_of_birth: DOB.format("YYYY-MM-DD")
    }

    try{
      const response = await apiFetch('POST', '/api/user/register', body)
      setToken(response.auth_token)
      navigate("/")
    }
    catch(error) {
      console.log(error)
      setErrorMsg(error.reason)
      setErrorStatus(true)
      setFieldInState('errorMsg', "Email taken", email, setEmail)
      setFieldInState('error', true, email, setEmail)
      setFieldInState('forceError', true, email, setEmail)
    }
  }

  return (
    <div>
      <StandardLogo/>
      <Box disabled className='Background'> 
        <Box disabled sx={{ boxShadow: 3, width:500, borderRadius:2, backgroundColor: '#F5F5F5', padding: 1}} >
          <H3
            sx={{
              fontSize: '30px'
            }}
          >
            Create an account
          </H3>
          <Divider/>
          <br/>
          <FormInput>
            <Grid container spacing={2}>
              <Grid xs={6}>
                <OutlinedInput id="first_name" placeholder="First Name" variant="outlined" onChange={firstNameChange} sx={{borderRadius: 2}} error={firstName.error}/>
                {/* <TextField id="first_name" label="First Name" variant="outlined" onChange={firstNameChange} size="small"/> */}
              </Grid>
              <Grid xs={6}>
                <OutlinedInput id="last_name" placeholder="Last Name" variant="outlined" onChange={lastNameChange} sx={{borderRadius: 2}} error={lastName.error}/>
              </Grid>
              <Grid xs={12}>
                <FormControl fullWidth={true}>
                  <OutlinedInput 
                    id="email"
                    placeholder="Email"
                    variant="outlined"
                    onChange={EmailChange}
                    error={email.error}
                    sx={{borderRadius: 2}}
                  />
                  <EmailHelperText/>
                </FormControl>
              </Grid>
              <Grid xs={6}>
                <FormControl >
                  <OutlinedInput 
                    id='password'
                    placeholder='Password'
                    variant="outlined"
                    type={!password.visibility ? "password" : "text"}
                    error={password.error}
                    onChange={PasswordChange}
                    sx={{borderRadius: 2}}
                    endAdornment={
                      <InputAdornment position="end">
                        <Tooltip title={password.visibility ? "Hide password" : "Show Password"}>
                          <IconButton
                            aria-label="toggle password visibility"
                            onClick={(e) => setFieldInState('visibility', !password.visibility, password, setPassword)}
                            onMouseDown={(e) => e.preventDefault()}
                            edge="end"
                          >
                            {password.visibility ? <VisibilityOff /> : <Visibility />}
                          </IconButton>
                        </Tooltip>
                      </InputAdornment>
                    }
                  />
                  <PasswordHelperText/>
                </FormControl>
              </Grid>
              <Grid xs={6}>
                <FormControl>
                  <OutlinedInput
                    id='confirmPassword'
                    placeholder='Confirm Password'
                    variant="outlined"
                    type="password"
                    error={confirmPassword.error}
                    onChange={ConfirmPasswordChange}
                    disabled={confirmPassword.disabled}
                    sx={{borderRadius: 2}}
                  />
                  <FormHelperText>{confirmPassword.errorMsg}</FormHelperText>
                </FormControl>
              </Grid>
              <Grid xs={3}>
              </Grid>
              <Grid xs={6}>
                <OutlinedInput id="user_name" placeholder="Display Name" variant="outlined" onChange={userNameChange} fullWidth={true} sx={{borderRadius: 2}} error={userName.error}/>
              </Grid>
              <Grid xs={3}>
              </Grid>
              <Grid xs={3}>
              </Grid>
              <Grid xs={6}>
                  <LocalizationProvider dateAdapter={AdapterDayjs}>
                  <DesktopDatePicker
                    label="Date of Birth"
                    inputFormat="DD/MM/YYYY"
                    value={DOB}
                    onChange={setDOB}
                    renderInput={(params) => <TextField {...params} />}
                    disableFuture = {true}
                  />
                </LocalizationProvider>
              </Grid>
              <Grid xs={3}>
              </Grid>
            </Grid>
          </FormInput>
          <br/>
          <Divider/>
          <br/>
          <FormInput>
            <TkrButton
              variant="contained"
              onClick={SubmitRegister}
            >
              Register
            </TkrButton>
            <Collapse in={errorStatus}>
              <Alert severity="error">{errorMsg}.</Alert>
            </Collapse>
            <TextButton component={Link} to="/login" >Already have an account?</TextButton>
          </FormInput>
          <br/>
        </Box>
      </Box>
    </div>
  )
}