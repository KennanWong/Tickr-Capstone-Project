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
import { FlexRow, Logo, H3 } from '../Styles/HelperStyles';
import HelperText from '../Components/HelperText';
import { Link, useNavigate } from "react-router-dom";
import StandardLogo from "../Components/StandardLogo";


export default function PageNotFound({}) {
  return (
    <div>
      <StandardLogo/>
      <div>
      <Box disabled className='Background'> 
        <Box disabled sx={{ boxShadow: 3, width:500, borderRadius:2, backgroundColor: '#F5F5F5', padding: 1}} >
          <H3
            sx={{
              fontSize: '30px',
            }}
          >
            404 Page Not Found
          </H3>
        </Box>
      </Box>
      </div>
    </div>
  )
}