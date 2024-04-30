import React from 'react'
import { styled, alpha } from '@mui/system';
import Box from '@mui/material/Box';
import AppBar from '@mui/material/AppBar';
import { IconButton } from '@mui/material';

export const TickrName = styled('div')({
  height: '50px',
  fontHeight: '100pt'
});

export const FlexRow = styled('div')({
  display: 'flex',
  justifyContent: 'center',
  padding: '10px',
  margin: 'auto',
  alignItems: 'center',
  gap: '5px',
});

export const H3 = styled('h3')({
  textAlign: 'center'
})

export const Logo = styled('img')({
  padding: '20px',
  width: '100px',
  height: 'auto',
  position: 'fixed',
})

export const Backdrop = styled(Box)({
  backgroundImage: 'linear-gradient(135deg,#AFDEDC, #CAA5C0)',
  width: '100%',
  backgroundSize: 'cover',
  margin: 'auto',
  minHeight: '100vh',
  height: '100%',
  padding: '10px',
  position: 'fixed',
})

export const BackdropNoBG = styled(Box)({
  width: '100%',
  backgroundSize: 'cover',
  margin: 'auto',
  minHeight: '100vh',
  height: '100%',
  backgroundColor: '#F1F9F9',
})

export const BackdropNoBG_VH = styled(Box)({
  width: '100%',
  backgroundSize: 'cover',
  margin: 'auto',
  minHeight: '100vh',
  height: '100vh',
  backgroundColor: '#F1F9F9',
})

export const CentredBox = styled(Box)({
  display: "flex",
  alignItems:'center',
  justifyContent: 'center'
})

export const ContentBox = styled(Box)({
  width: '90%',
  marginLeft: 'auto',
  marginRight: 'auto',
  backgroundColor: 'white',
  padding: '10px',
})

export const HeaderBar = styled(AppBar)({
  background: "#AFDEDC",
  position: "sticky",
  padding: '10px',
  height: '70px'
})

export const UploadPhoto = styled(`img`)({
  height: '100%',
  width: '100%',
  objectFit: 'cover',
  borderRadius: 5,
})

export const ScrollableBox = styled(Box)({
  overflowY: 'scroll',
  '&::-webkit-scrollbar': {
    width: 10
  },
  '&::-webkit-scrollbar-track': {
    boxShadow: 'inset 0 0 6px rgba(0,0,0,0.00)',
    webkitBoxShadow: 'inset 0 0 6px rgba(0,0,0,0.00)',
    backgroundColor: alpha('#6A7B8A', 0.3),
    borderRadius: 5
  },
  '&::-webkit-scrollbar-thumb': {
    backgroundColor: '#6A7B8A',
    borderRadius: 5
  }
})

export const ExpandMore = styled((props) => {
  const { expand, ...other } = props;
  return <IconButton {...other} />;
})(({ theme, expand }) => ({
  transform: !expand ? 'rotate(0deg)' : 'rotate(180deg)',
  marginLeft: 'auto',
  transition: theme.transitions.create('transform', {
    duration: theme.transitions.duration.shortest,
  }),
}));