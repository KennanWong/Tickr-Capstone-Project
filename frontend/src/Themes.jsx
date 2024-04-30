import { ThemeProvider, createTheme,  alpha} from '@mui/material/styles';
import { pink } from '@mui/material/colors';

export const TickrTheme = createTheme({
  components: {
    // Name of the component
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          "&:hover .MuiOutlinedInput-notchedOutline": {
            borderColor: "#AE759F"
          },
          "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
            borderColor: "#AE759F"
          }
        },
      }
    },
    MuiAvatar: {
      styleOverrides: {
        root: {
          backgroundColor: '#AE759F'
        }
      }
    },
    MuiFormLabel: {
      styleOverrides: {
        root: {
          "&:focused": {
            color: 'grey'
          }
        }
      }
    },
    MuiCheckbox: {
      styleOverrides: {
        root: {
          color: "#AE759F",
          "&.Mui-checked" : {
            color: "#AE759F",
          }
        }
      }
    },
    MuiSwitch: {
      styleOverrides: {
        root: {
          '& .MuiSwitch-switchBase.Mui-checked': {
            color: '#73B5D3',
            '&:hover': {
              backgroundColor: alpha('#73B5D3', 0.1),
            },
          },
          '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': {
            backgroundColor: '#73B5D3',
          },
        }
      }
    },
    MuiTypography: {
      allVariants: {
        fontFamily: 'Segoe UI'
      }
    },
    MuiStepIcon: {
      root: {
        '&.active': {
          color: 'red',
          },
      }
    }
  },
});