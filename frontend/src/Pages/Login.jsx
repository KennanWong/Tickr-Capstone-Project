import React from "react";

import Box from '@mui/material/Box';
import Divider from '@mui/material/Divider';
import TextField from '@mui/material/TextField';
import OutlinedInput from '@mui/material/OutlinedInput';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { MobileDatePicker } from '@mui/x-date-pickers/MobileDatePicker';
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
import logo from '../Images/TickrLogo.png'

import { FormInput, TextButton, TkrButton} from '../Styles/InputStyles';

import '../App.css';

import { apiFetch, setFieldInState, setToken } from '../Helpers';
import { FlexRow, Logo, H3, CentredBox } from '../Styles/HelperStyles';
import HelperText from '../Components/HelperText';
import { Link, useNavigate, useParams } from "react-router-dom";
import StandardLogo from "../Components/StandardLogo";
import { Tooltip } from "@mui/material";


export default function Login({customNavigateTo=false}) {
  const params = useParams()

  const [email, setEmail] = React.useState({
    email: '',
    error: false,
    errorMsg: ''
  })

  const [password, setPassword] = React.useState({
    password: '',
    visibility: false,
    error: false
  })

  const navigate = useNavigate()

  const [error, setError] = React.useState({
    state: false,
    msg: ''
  })

  var validEmail = /^(([^<>()[\]\.,;:\s@\"]+(\.[^<>()[\]\.,;:\s@\"]+)*)|(\".+\"))@(([^<>()[\]\.,;:\s@\"]+\.)+[^<>()[\]\.,;:\s@\"]{2,})$/i;
  const EmailChange = (e) => {
    setFieldInState('email', e.target.value, email, setEmail);
    setFieldInState('state', false, error, setError)
    if (!email.email.match(validEmail)) {
      setFieldInState('error', true, email, setEmail);
      setFieldInState('errorMsg', 'Enter a valid email', email, setEmail);
    } else {
      setFieldInState('error', false, email, setEmail);
      setFieldInState('errorMsg', '', email, setEmail);
    }
  };

  const PasswordChange = (e) => {
    setFieldInState('state', false, error, setError)
    setFieldInState('password', e.target.value, password, setPassword)
    setFieldInState('error', false, password, setPassword)
  }
  
  const submitLogin = async () => {

    const body = {
      email: email.email,
      password: password.password
    }

    try {
      const response = await apiFetch('POST', '/api/user/login', body)
      setToken(response.auth_token)
      if (customNavigateTo) {
        navigate(`/ticket/purchase/group/${params.invite_id}`)
      } else {
        navigate("/")
      }
    } catch (errorResponse) {
      console.log(errorResponse.reason)
      setFieldInState('state', true, error, setError)
      setFieldInState('msg', errorResponse.reason, error, setError)
      setFieldInState('error', true, email, setEmail)
      setFieldInState('error', true, password, setPassword)
    }
  }

  return (
    <div>
      <StandardLogo/>
      <div>
      <Box disabled className='Background'> 
        <Box disabled sx={{ boxShadow: 3, width:500, height: 500, borderRadius:2, backgroundColor: '#F5F5F5', padding: 1}} >
          <H3
            sx={{
              fontSize: '30px',
            }}
          >
            Login 
          </H3>
          <Divider/>
          <br/>
          <FormInput>
            <Grid container spacing={2} sx={{paddingLeft: 5, paddingRight: 5}}>
              <Grid xs={12}>
                <FormControl fullWidth={true}>
                  <OutlinedInput
                    id="email"
                    placeholder="Email"
                    variant="outlined"
                    sx={{borderRadius: 2}} 
                    fullWidth={true} 
                    error={email.error}
                    onChange={EmailChange}
                  />
                  <FormHelperText>{email.errorMsg}</FormHelperText>
                </FormControl>
              </Grid>
              <Grid xs={12}>
                <FormControl fullWidth={true}>
                  <OutlinedInput
                    id="password"
                    placeholder="Password"
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
                </FormControl>
              </Grid>
            </Grid>
          </FormInput>
          <br/>
          <Divider/>
          <br/>
          <FormInput>
            <TkrButton
              variant="contained"
              onClick={submitLogin}
            >
              Sign in
            </TkrButton>
            <Collapse in={error.state}>
              <Alert severity="error">{error.msg}</Alert>
            </Collapse>
            <CentredBox sx={{flexDirection: 'row'}}>
              <TextButton component={Link} to="/request_change_password">Forgot password?</TextButton>
              <Divider orientation="vertical" variant="middle" flexItem/>
              <TextButton component={Link} to="/register">Don&#39;t have an account?</TextButton>
            </CentredBox>
          </FormInput>
        </Box>
      </Box>
      </div>
    </div>
  )
}