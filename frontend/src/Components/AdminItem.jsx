import React from "react"

import { apiFetch, checkValidEmail, getToken, getUserData, setFieldInState } from "../Helpers";
import Grid from "@mui/material/Unstable_Grid2";
import { H3 } from "../Styles/HelperStyles";
import ListItemText from "@mui/material/ListItemText";
import DeleteIcon from "@mui/icons-material/Delete";
import IconButton from "@mui/material/IconButton";
import { Box, Divider, FormLabel, List, ListItem, Tooltip } from "@mui/material";
import { borderRadius, styled, alpha } from '@mui/system';
import { ContrastInput, ContrastInputWrapper, DeleteButton, FormInput, TextButton, TkrButton } from '../Styles/InputStyles';
import ListItemIcon from '@mui/material/ListItemIcon';
import UserAvatar from "./UserAvatar";
import Skeleton from '@mui/material/Skeleton';
import Person4Icon from '@mui/icons-material/Person4';

export default function AdminItem({index, adminID, removeAdmin, newAdmins=[], editEvent=false, openHostMenu=null, setNewHost=null}) {
  const [userData, setUserData] = React.useState({
    userName: '',
    firstName: '',
    lastName: '',
    email: '',
    profilePicture: '',
    events: []
  })

  React.useEffect(()=> {
    getUserData(`user_id=${adminID}`,setUserData)
  },[])

  const handleMakeHost = () => {
    setNewHost(userData)
    openHostMenu(true)
  }

  return (
    <ListItem 
      secondaryAction={
        <>
          {editEvent
            ? <Box
                sx={{
                  display: 'flex'
                }}
              > 
                {/* Check if admin is newly added, event must be first saved */}
                {(newAdmins.includes(adminID))
                  ? <Tooltip title="Save the event before making user a host.">
                      <span>
                        <IconButton disabled
                          >
                            <Person4Icon/>
                          </IconButton>
                      </span>              
                    </Tooltip>
                  : <Tooltip title="Make Host">
                      <IconButton
                        onClick={handleMakeHost}
                      >
                        <Person4Icon/>
                      </IconButton>
                    </Tooltip>
                }
                <Divider orientation="vertical" variant="middle" flexItem/>
                <Tooltip title="Remove Admin">
                  <IconButton
                    edge="end"
                    aria-label="delete"
                    onClick={() => removeAdmin(index)}
                  >
                    <DeleteIcon />
                  </IconButton>
                </Tooltip>
              </Box>
            : <Tooltip title="Remove Admin">
                <IconButton
                  edge="end"
                  aria-label="delete"
                  onClick={() => removeAdmin(index)}
                >
                  <DeleteIcon />
                </IconButton>
              </Tooltip>
          }
        </>
      }
    >
      <ListItemIcon>
        <UserAvatar userId={adminID} size={50}/>
      </ListItemIcon>
      {(userData.userName === '')
        ? <Skeleton animation="wave"  width={"100%"}/>
        : <ListItemText primary={`@${userData.userName}`}/>
      }
      
    </ListItem>
  )
}