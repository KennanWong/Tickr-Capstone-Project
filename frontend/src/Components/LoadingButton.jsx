import React from "react";

import Box from '@mui/material/Box';
import Divider from '@mui/material/Divider';
import FormControl, { useFormControl } from '@mui/material/FormControl';
import FormHelperText from '@mui/material/FormHelperText';
import Grid from '@mui/material/Unstable_Grid2';
import Alert from '@mui/material/Alert';
import Collapse from '@mui/material/Collapse';
import FormLabel from '@mui/material/FormLabel';
import {CircularProgress} from '@mui/material';

import { FormInput, TextButton, TkrButton} from '../Styles/InputStyles';

import '../App.css';

import { apiFetch, getToken, loggedIn, passwordCheck, setFieldInState } from '../Helpers';
import { FlexRow, Logo, H3, CentredBox } from '../Styles/HelperStyles';
import HelperText from '../Components/HelperText';
import { Link, useNavigate, useParams } from "react-router-dom";
import StandardLogo from "../Components/StandardLogo";
import PasswordInput from "../Components/PasswordInput";


export default function LoadingButton({label, method, sx={}, route, body, navigateTo=null, func = null, funcVal = null, startIcon=null, state=null, setState=null, disabled=false}) {
  const navigate = useNavigate()

  const [loading, setLoading] = React.useState(false)

  const handleSubmit = async () => {
    setLoading(true)
    try {
      const response = await apiFetch(method, route, body)
      if (func != null) {
        func(funcVal)
      }
      if (state !== null) {
        setFieldInState('responseStatus', true, state, setState)
        setFieldInState('response', response, state, setState)
      }
      setLoading(false)
      if (navigateTo == null) {
        window.location.reload(false);
      } else {
        navigate(navigateTo)
      }
    } catch (e) {
      if (state != null) {
        setFieldInState('error', true, state, setState)
        setFieldInState('errorMsg', e.reason, state, setState)
        setLoading(false)
      }
    }
  }

  return (
    <CentredBox sx={{position: 'relative'}}>
      <TkrButton
        variant="contained"
        disabled={(loading || disabled)}
        sx={sx}
        onClick={handleSubmit}
        startIcon={startIcon}
      >
        {label}
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
  )
}