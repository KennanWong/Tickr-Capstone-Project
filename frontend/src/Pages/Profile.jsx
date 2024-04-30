import React from 'react'

import { BackdropNoBG, CentredBox, UploadPhoto  } from '../Styles/HelperStyles'
import Grid from '@mui/material/Grid';

import Header from '../Components/Header'
import { Box } from '@mui/system';
import { Avatar, Button, Divider, Typography } from '@mui/material';
import EmailIcon from '@mui/icons-material/Email';
import EditIcon from '@mui/icons-material/Edit';
import SaveIcon from '@mui/icons-material/Save';
import DeleteIcon from '@mui/icons-material/Delete';
import Tooltip from '@mui/material/Tooltip';
import { ContrastInput, ContrastInputWrapper, DeleteButton, TkrButton } from '../Styles/InputStyles';
import { setFieldInState, getToken, getUserData, apiFetch, fileToDataUrl } from '../Helpers';
import ShadowInput from '../Components/ShadowInput';
import LinearProgress from '@mui/material/LinearProgress';
import FormLabel from '@mui/material/FormLabel';
import FormControl from '@mui/material/FormControl';
import FormGroup from '@mui/material/FormGroup';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import ConfirmPassword from '../Components/ConfirmPassword';
import PhotoCamera from '@mui/icons-material/PhotoCamera';
import { alpha } from '@mui/system';
import { Link, useParams, } from 'react-router-dom';
import LoadingButton from '../Components/LoadingButton';
import HostEvents from '../Components/HostEvents';

export default function Profile({editable=false}){
  const params = useParams()

  const [hover, setHover] = React.useState(false)

  const [editMode, setEditMode] = React.useState(false)

  const [profile, setProfile] = React.useState({
    userName: '',
    firstName: '',
    lastName: '',
    email: '',
    profilePicture: '',
    profilePictureChanged: false,
    events: []
  })

  const [notifications, setNotifications] = React.useState({
    reminders: false,
  });

  const setNotificationsVal = (settings) => {
    for (const option in settings) {
      setFieldInState(option, settings[option], notifications, setNotifications)
    }
  }

  const getNotifications = async () => {
    try {
      const response = await apiFetch('GET', `/api/user/settings?auth_token=${getToken()}`)
      const settings = response.settings
      setNotifications(settings)
    } catch (e) {
      console.log(e)
    }
    
    return
  }

  const [changePW, setChangePW] = React.useState(false)

  const [loading, setLoading] = React.useState(false)

  const handleChangePW = (e) => {
    setChangePW(!changePW)
  }

  React.useEffect(() => {
    if (editable) {
      getNotifications()
      getUserData(`auth_token=${getToken()}`, setProfile)
    } else {
      getUserData(`user_id=${params.user_id}`, setProfile)
    }
    
  }, [editMode])

  const editModeChange = (e) => {
    e.stopPropagation()
    e.nativeEvent.stopImmediatePropagation()
    setEditMode(!editMode)
  }
  

  const saveChanges = (e) => {
    e.stopPropagation()
    e.nativeEvent.stopImmediatePropagation()
    setEditMode(!editMode)

  }

  const discardChanges = (e) => {
    e.stopPropagation()
    e.nativeEvent.stopImmediatePropagation()
    setEditMode(!editMode)
  }

  const notificationChange = async (e) => {
    setFieldInState(e.target.name, e.target.checked, notifications, setNotifications)
    // Send API call
    try {
      const body = {
        auth_token: getToken(),
        settings: notifications
      }
      const response = await apiFetch('PUT', '/api/user/settings/update', body)
    } catch (e) {
      console.log(e)
    }
  }

  const [delAcc, setDelAcc] = React.useState(false)

  const handleDelAcc = (e) => {
    setDelAcc(!delAcc)
  }

  const packageBody = () => {
    const body = {
      auth_token: getToken(),
      user_name: profile.userName,
      first_name: profile.firstName,
      last_name: profile.lastName,
      email: profile.email,
      profile_description: profile.profileDescription,
      profile_picture: profile.profilePictureChanged ? profile.profilePicture : null,
    }

    return body
  }

  return (
    <div>
      <BackdropNoBG >
        <Header/>
        <Box
          sx={{
            minHeight: 600,
            maxWidth: 1500,
            marginLeft: 'auto',
            marginRight: 'auto',
            width: '95%',
            backgroundColor: '#FFFFFF',
            marginTop: '50px',
            borderRadius: '15px',
            boxShadow: editMode ? '5' : 0,
            paddingBottom: 5,
          }}
        >
          {(profile.userName == '')
            ? <Box sx={{height: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center', margin: '50px', paddingTop: '100px'}}> 
                <Box sx={{ width: '90%', hieght: 50}}>
                  <LinearProgress color='secondary'/>
                </Box>
              </Box>
            : <Grid container spacing={2}>
                <Grid item xs={4}>
                  <Box 
                    sx={{
                      height: '100%',
                      marginTop: '10px',
                      marginLeft: '30px',
                      display: 'flex',
                      alignItems: 'flex-start',
                      justifyContent: 'center',
                    }}
                  >
                    {editMode
                      ? <CentredBox
                          sx={{
                            width: '300px',
                            height: '300px',
                            borderRadius: '150px',
                            backgroundColor: alpha('#6A7B8A', 0.5),
                          }}
                          onMouseOver={() => {
                            setHover(false);
                          }}
                          onMouseOut={() => {
                            setHover(true);
                          }}
                        >
                          {!hover
                            ? <Button
                                sx={{
                                  backgroundColor: 'rgba(0,0,0,0)',
                                  "&:hover": {
                                    backgroundColor: 'rgba(0,0,0,0)',
                                  },
                                  fontSize: 20,
                                  color: 'white'
                                }}
                                startIcon={<PhotoCamera/>}
                                component="label"
                              >
                                Upload photo
                                <input
                                  hidden
                                  accept="image/*"
                                  type="file" 
                                  onChange={async (e) => {
                                    const image = await fileToDataUrl(e.target.files[0])
                                    setFieldInState("profilePictureChanged", true, profile, setProfile)
                                    setFieldInState("profilePicture", image, profile, setProfile)
                                  }}
                                />
                              </Button>
                            : <>
                                {(profile.profilePicture !== "")
                                  ? <UploadPhoto sx={{borderRadius: 300}} src={profile.profilePicture}/>
                                  : <Avatar sx={{width: 300, height: 300, fontSize: 100}}>
                                      {profile.firstName[0]}{profile.lastName[0]}
                                    </Avatar>
                                } 
                              </>
                          }
                        </CentredBox>
                      : <CentredBox
                          sx={{
                            width: '300px',
                            height: '300px',
                            borderRadius: '300px',
                          }}
                        >
                          {(profile.profilePicture !== "")
                            ? <UploadPhoto sx={{borderRadius: 300}} src={profile.profilePicture}/>
                            : <Avatar sx={{width: 300, height: 300, fontSize: 100}}>
                                {profile.firstName[0]}{profile.lastName[0]}
                              </Avatar>
                          } 
                          
                        </CentredBox> 
                    }
                  </Box>
                </Grid>
                <Grid item xs={6}>
                  {editMode
                    ? <CentredBox sx={{justifyContent: 'flex-start', gap: '10px', height: '60px', alignItems: 'baseline'}}>
                        <Grid container spacing={2}>
                          <Grid item xs={4}>
                            <ShadowInput sx={{fontWeight: 'bold'}} state={profile} setState={setProfile} defaultValue={profile.firstName} field='firstName' placeholder='First Name'/>
                          </Grid>
                          <Grid item xs={4}>
                            <ShadowInput sx={{fontWeight: 'bold'}} state={profile} setState={setProfile} defaultValue={profile.lastName} field='lastName' placeholder='Last Name'/>
                          </Grid>
                          <Grid item xs={3}>
                            <Box sx={{display: 'flex', height: '100%', alignItems: 'flex-end'}}>
                              <ContrastInputWrapper>
                                <ContrastInput size='small' defaultValue={profile.userName} startAdornment={<div>@</div>} fullWidth placeholder='Display Name' onChange={(e) => {
                                  setFieldInState('userName', e.target.value, profile, setProfile)
                                }}/>
                              </ContrastInputWrapper>
                            </Box>
                          </Grid>
                        </Grid>
                      </CentredBox>
                    : <CentredBox sx={{justifyContent: 'flex-start', gap: '10px', height: '60px', alignItems: 'baseline'}}>
                        <Typography 
                          sx={{
                            fontSize: '40px',
                            fontWeight: 'bold'
                          }}
                        >
                          {profile.firstName} {profile.lastName}
                        </Typography>
                        <Divider orientation="vertical" variant="middle" flexItem/>
                        <Typography 
                          sx={{
                            fontSize: '20px',
                            fontWeight: 'light',
                            fontStyle: 'italic',
                            color: '#454545',
                          }}
                        >
                          @{profile.userName}
                        </Typography>
                      </CentredBox>
                  }
                  <Divider/>
                  <br/>
                  <Box 
                    sx={{
                      height: '100%',
                    }}
                  >
                    {editMode
                      ? <Box
                          sx={{
                            padding: editMode ? 0 : '10px',
                            borderRadius: '10px',
                            backgroundColor: '#F1F9F9',
                          }}
                        > 
                          <ContrastInputWrapper>
                            <ContrastInput multiline placeholder={'Enter a description'} rows={4} defaultValue={profile.profileDescription} fullWidth onChange={(e) => {
                                setFieldInState('profileDescription', e.target.value, profile, setProfile)
                              }}/>
                          </ContrastInputWrapper>
                        </Box>
                      : <div>
                          {(profile.profileDescription != '')
                            ? <Box
                                sx={{
                                  padding: editMode ? 0 : '10px',
                                  borderRadius: '10px',
                                  backgroundColor: '#F1F9F9',
                                }}
                              > 
                                <Typography
                                  sx={{
                                    fontSize: '20px',
                                    fontWeight: 'regular',
                                    fontStyle: 'italic',
                                  }}
                                >
                                  {profile.profileDescription}
                                </Typography>
                                </Box>
                            : <div></div>
                          }
                        </div>
                    }
                    <br/>
                    <Grid container spacing={2}>
                      <Grid item xs={3} sx={{display: 'flex', alignItems: 'center'}}>
                        <CentredBox sx={{gap: '5px', justifyContent: 'flex-start'}}>
                          <EmailIcon sx={{color: '#AE759F'}}/>
                          <Typography
                            sx={{
                              fontSize: '20px',
                              fontWeight: 'bold',
                              color: '#AE759F',
                            }}
                          >
                            Email
                          </Typography>
                        </CentredBox>
                      </Grid>
                      <Grid item xs={9}>
                        {editMode
                          ? <ShadowInput state={profile} setState={setProfile} defaultValue={profile.email} field='email' placeholder='Email'/>
                          : <Typography
                              sx={{
                                fontSize: '20px',
                                fontWeight: 'regular'
                              }}
                            >
                              {profile.email}
                            </Typography>
                        }
                      </Grid>
                    </Grid>
                    <br/>
                  </Box>
                </Grid>
                <Grid item xs={2}>
                  <Box
                    sx={{
                      display: 'flex',
                      paddingLeft: '15px',
                      marginTop: '20px'
                    }}
                  >
                    {editable
                      ? <>
                          {(editMode)
                            ? <LoadingButton
                                label={"Save"}
                                method={'PUT'}
                                sx={{height: 30, width: 90, fontSize: 20, textTransform: "none", textAlign: "left"}}
                                startIcon={<SaveIcon/>}
                                route={"/api/user/editprofile"}
                                body={packageBody()}
                                func={setEditMode} 
                                funcVal={false}
                              />
                            : <TkrButton variant='text' startIcon={<EditIcon/>} sx={{height: 30, width: 90, fontSize: 20, textTransform: "none", textAlign: "left"}} onClick={editModeChange}>
                                Edit
                              </TkrButton>
                          }
                        </>
                      : <></>

                    }
                  </Box>
                  {editMode
                    ? <Box
                        sx={{
                          display: 'flex',
                          paddingLeft: '15px',
                          marginTop: '20px'
                        }}
                      >
                        <Tooltip title="Discard changes">
                          <DeleteButton variant='text' startIcon={<DeleteIcon/>} sx={{height: 30, width: 90, fontSize: 15, textTransform: "none", textAlign: "left"}} onClick={discardChanges}>
                            Discard
                          </DeleteButton>
                        </Tooltip>
                      </Box>
                    : <div></div>
                  }
                </Grid>
                <Grid item xs={4}></Grid>
                <Grid item xs={7}>
                  {editable
                    ? <Box sx={{}}>
                        <Typography
                          sx={{
                            fontSize: '20px',
                            fontWeight: 'regular'
                          }}
                        > Account Settings</Typography>
                        <Divider/>
                        <FormControl sx={{paddingTop: 2, width: 250}} component="fieldset" variant="standard">
                          <FormLabel sx={{"&.Mui-focused": {color: 'rgba(0, 0, 0, 0.6) '}}}>Notification</FormLabel>
                          <Divider variant="fullWidth"/>
                          <FormGroup>
                            <FormControlLabel
                              control={
                                <Checkbox  name="reminders" checked={notifications.reminders} onChange={notificationChange}/>
                              }
                              label="Enable email reminders"
                            />
                          </FormGroup>
                        </FormControl>
                        <br/>
                        <FormControl sx={{paddingTop: 2}} component="fieldset" variant="standard">
                          <FormLabel sx={{"&.Mui-focused": {color: 'rgba(0, 0, 0, 0.6) '}}}>Password Management</FormLabel>
                          <Divider variant="fullWidth"/>
                          <Box sx={{paddingTop: 1, width: 250}}>
                            <TkrButton variant='text' sx={{height: 30, fontSize: 20, textTransform: "none"}} onClick={handleChangePW} component={Link} to="/change_password">Change Password</TkrButton>
                          </Box>
                        </FormControl>
                        <br/>
                        <FormControl sx={{paddingTop: 2, width: 250}} component="fieldset" variant="standard">
                          <FormLabel sx={{"&.Mui-focused": {color: 'rgba(0, 0, 0, 0.6) '}}}>Account Management</FormLabel>
                          <Divider variant="fullWidth"/>
                          <Box sx={{paddingTop: 1}}>
                            <DeleteButton 
                              variant='text'
                              sx={{
                                height: 30,
                                fontSize: 20, 
                                textTransform: "none", 
                                textAlign: "left", 
                                "&:hover": {
                                  backgroundColour: "#AA4344"
                                }
                              }} 
                              startIcon={<DeleteIcon/>}
                              onClick = {handleDelAcc}
                            >
                              Delete Account
                            </DeleteButton>
                            <ConfirmPassword open={delAcc} handleOpen={handleDelAcc} method={'DELETE'} route={'/api/user/delete'} navigateTo={'/'}/>
                          </Box>
                        </FormControl>
                      </Box>
                    : <div></div>
                  }
                </Grid>
                <Grid item xs={4}></Grid>
                <Grid item xs={7}>
                  <br/>
                  <Typography sx={{color: 'rgba(0, 0, 0, 0.6)'}}>
                    Hosted/Hosting Events
                  </Typography>
                  <Divider width={245}/>
                  <HostEvents userDetails={profile}/>
                </Grid>
              </Grid>
          }
        </Box>
        
      </BackdropNoBG>
    </div>
  )
}