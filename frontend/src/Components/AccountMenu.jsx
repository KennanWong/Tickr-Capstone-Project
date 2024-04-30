import * as React from 'react';
import Box from '@mui/material/Box';
import Avatar from '@mui/material/Avatar';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import Tooltip from '@mui/material/Tooltip';
import PersonAdd from '@mui/icons-material/PersonAdd';
import Settings from '@mui/icons-material/Settings';
import Logout from '@mui/icons-material/Logout';
import EventIcon from '@mui/icons-material/Event';
import HomeIcon from '@mui/icons-material/Home';

import { Link, useNavigate } from 'react-router-dom';
import { getToken, setToken, getUserData } from '../Helpers';
import { UploadPhoto } from '../Styles/HelperStyles';
import { Skeleton } from '@mui/material';


export default function AccountMenu() {

  const navigate = useNavigate()

  const [isLoggedIn, setIsLoggedIn] = React.useState((getToken() == null))

  const [anchorEl, setAnchorEl] = React.useState(null);
  const open = Boolean(anchorEl);
  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };
  const handleClose = () => {
    setAnchorEl(null);
  };

  const doLogout = async (e) => {
    e.stopPropagation()
    e.nativeEvent.stopImmediatePropagation()
    try {
      const response = await('DELETE', '/api/user/logout', {auth_token: getToken()})
      setToken(null)
      setIsLoggedIn(false)
      navigate('/')
      window.location.reload(false);
    } catch (e) {
      console.log(e)
    }
  };

  const [userData, setUserData] = React.useState({
    firstName: '',
    lastName: '',
    userName: '',
    email: '',
    profileDescription: '',
    events: [],
  })

  React.useEffect(() => {
    getUserData(`auth_token=${getToken()}`, setUserData)
  }, []);

  return (
    <React.Fragment>
      <Box sx={{ display: 'flex', alignItems: 'center', textAlign: 'center' }}>
        <Tooltip title="Account settings">
          <IconButton
            onClick={handleClick}
            size="small"
            sx={{ ml: 2 }}
            aria-controls={open ? 'account-menu' : undefined}
            aria-haspopup="true"
            aria-expanded={open ? 'true' : undefined}
          >
            {(userData.userName !== "")
              ? <>
                  {(userData.profilePicture !== "")
                    ? <UploadPhoto sx={{width: 37, height: 37, borderRadius: 37}} src={userData.profilePicture}/>
                    : <Avatar sx={{ width: 37, height: 37 }}>{userData.firstName[0]}{userData.lastName[0]}</Avatar>
                  }
                </>
              : <Skeleton variant="circular" width={37} height={37}/>
            }
             
          </IconButton>
        </Tooltip>
      </Box>
      <Menu
        anchorEl={anchorEl}
        id="account-menu"
        open={open}
        onClose={handleClose}
        onClick={handleClose}
        PaperProps={{
          elevation: 0,
          sx: {
            overflow: 'visible',
            filter: 'drop-shadow(0px 2px 8px rgba(0,0,0,0.32))',
            mt: 1.5,
            '& .MuiAvatar-root': {
              width: 35,
              height: 35,
              ml: -0.5,
              mr: 1,
            },
            '&:before': {
              content: '""',
              display: 'block',
              position: 'absolute',
              top: 0,
              right: 14,
              width: 10,
              height: 10,
              bgcolor: 'background.paper',
              transform: 'translateY(-50%) rotate(45deg)',
              zIndex: 0,
            },
          },
        }}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        disableScrollLock={true}
      >
        <MenuItem component={Link} to="/">
          <ListItemIcon>
            <HomeIcon fontSize="medium"/>
          </ListItemIcon>
          Home
        </MenuItem>
        <MenuItem component={Link} to="/my_profile">
          <Avatar  sx={{backgroundColor: "#999999"}}/> View Profile
        </MenuItem>
        <MenuItem component={Link} to="/create_event">
          <ListItemIcon>
            <EventIcon fontSize="medium"/>
          </ListItemIcon>
          Create Event
        </MenuItem>
        <Divider></Divider>
        <MenuItem onClick={doLogout}>
          <ListItemIcon>
            <Logout fontSize="small" />
          </ListItemIcon>
          Logout
        </MenuItem>
      </Menu>
    </React.Fragment>
  );
}
