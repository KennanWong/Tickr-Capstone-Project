import React from "react";

import OutlinedInput from '@mui/material/OutlinedInput';
import InputAdornment from '@mui/material/InputAdornment';
import IconButton from '@mui/material/IconButton';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';

import { FormInput, TextButton, TkrButton} from '../Styles/InputStyles';

import '../App.css';

import { apiFetch, setFieldInState, setToken } from '../Helpers';
import { FlexRow, Logo, H3 } from '../Styles/HelperStyles';
import HelperText from '../Components/HelperText';
import { Link, useNavigate } from "react-router-dom";
import StandardLogo from "../Components/StandardLogo";
import { Tooltip } from "@mui/material";


export default function PasswordInput ({state, setState, placeholder='Password', requirements=false, setError}) {

  const requirementsCheck = () => {
    var hasUpper = state.password.match(/[A-Z]/);
    var hasDigit = state.password.match(/[0-9]/);
    var hasSpecial = state.password.match(/[!@#$%^&*]/);
    var hasLength = (state.password.length >= 8);

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

    setFieldInState('error', !validPassword, state, setState)
    if (!validPassword) {
      setFieldInState('errorMsg', errorMsg, state, setState)
    } else {
      setFieldInState('errorMsg', '', state, setState)
    }
  }

  const handleChange = (e) => {
    setFieldInState('password', e.target.value, state, setState)
    setFieldInState('error', false, state, setState)
    setError(false)
    if (requirements) {
      requirementsCheck()
    }
  }

  return (
    <OutlinedInput
      placeholder={placeholder}
      error={state.error}
      type={!state.visibility ? "password" : "text"}
      onChange={handleChange}
      sx={{borderRadius: 2}}
      endAdornment={
        <InputAdornment position="end">
          <Tooltip title={state.visibility ? "Hide password" : "Show Password"}>
            <IconButton
              aria-label="toggle password visibility"
              onClick={(e) => setFieldInState('visibility', !state.visibility, state, setState)}
              onMouseDown={(e) => e.preventDefault()}
              edge="end"
            >
              {state.visibility ? <VisibilityOff /> : <Visibility />}
            </IconButton>
          </Tooltip>
        </InputAdornment>
      }
    />
  )
}