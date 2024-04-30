import React from 'react'
import Avatar from '@mui/material/Avatar';
import Tooltip from '@mui/material/Tooltip';
import Skeleton from '@mui/material/Skeleton';
import { useNavigate } from 'react-router-dom';
import IconButton from '@mui/material/IconButton';
import Badge from '@mui/material/Badge';
import StarIcon from '@mui/icons-material/Star';
import { apiFetch, getToken, getUserData, loggedIn} from '../Helpers';
import { UploadPhoto } from '../Styles/HelperStyles';
import Person4Icon from '@mui/icons-material/Person4';
import { Typography } from '@mui/material';


export default function UserAvatar({userId, size=35, host=false}) {
  const navigate = useNavigate()

  const [userData, setUserData] = React.useState({
    userName: '',
    firstName: '',
    lastName: '',
    email: '',
    profilePicture: '',
    events: []
  })

  React.useEffect(()=> {
    if (userId !== undefined) {
      getUserData(`user_id=${userId}`,setUserData)
    }
    
  },[userId])


  const handleClick = async (e) => {
    e.stopPropagation();
    e.nativeEvent.stopImmediatePropagation();

    try {
      // Check if a uyser is logged in
      if (loggedIn()) {
        const response = await apiFetch('GET',`/api/user/profile?auth_token=${getToken()}`)
        const response_2 = await apiFetch('GET',`/api/user/search?email=${response.email}`)
        if (userId === response_2.user_id) {
          window.open('/my_profile')
          // navigate(`/my_profile`)
        } else {
          window.open(`/view_profile/${userId}`)
        }
      } else {
        window.open(`/view_profile/${userId}`)
      }
    } catch (e) {
      console.log(e)
    }
  }

  return (
    <IconButton disableRipple={true} onClick={handleClick}>
      {(userData.userName !== '')
        ? <>
            {host
              ? <Tooltip title= {`Event Host | @${userData.userName}`}>
                  <Badge 
                    anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                    badgeContent={<Person4Icon/>}
                  >
                    {(userData.profilePicture !== "")
                      ? <UploadPhoto sx={{width: size, height: size, borderRadius: size}} src={userData.profilePicture}/>
                      : <Avatar sx={{ width: size, height: size}}><Typography sx={{fontSize: {size}}}>{userData.firstName[0].toUpperCase()}{userData.lastName[0].toUpperCase()}</Typography></Avatar>
                    } 
                  </Badge>
                </Tooltip>
              : <Tooltip title={`@${userData.userName}`}>
                  {(userData.profilePicture !== "")
                    ? <UploadPhoto sx={{width: size, height: size, borderRadius: size}} src={userData.profilePicture}/>
                    : <Avatar sx={{ width: size, height: size}}><Typography sx={{fontSize: {size}}}>{userData.firstName[0].toUpperCase()}{userData.lastName[0].toUpperCase()}</Typography></Avatar>
                  } 
                </Tooltip>
            }
          </>
        : <Tooltip title="Unknown User">
            <Skeleton variant="circular" width={size} height={size} />
          </Tooltip>  
      }
    </IconButton>
    
  )
}