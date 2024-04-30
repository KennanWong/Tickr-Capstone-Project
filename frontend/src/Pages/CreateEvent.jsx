import React from "react";

import Header from "../Components/Header";
import { BackdropNoBG, CentredBox, UploadPhoto } from "../Styles/HelperStyles";
import { AdapterMoment } from "@mui/x-date-pickers/AdapterMoment";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider";
import { DateTimePicker } from "@mui/x-date-pickers/DateTimePicker";
import TextField from "@mui/material/TextField";
import dayjs from "dayjs";
import FormControl from "@mui/material/FormControl";
import FormHelperText from "@mui/material/FormHelperText";
import { apiFetch, checkValidEmail, getToken, getUserData, setFieldInState, fileToDataUrl, sortSection, isValidSpotifyURL } from "../Helpers";
import Grid from "@mui/material/Unstable_Grid2";
import { H3 } from "../Styles/HelperStyles";
import ListItemText from "@mui/material/ListItemText";
import DeleteIcon from "@mui/icons-material/Delete";
import IconButton from "@mui/material/IconButton";
import { Avatar, Backdrop, Box, Checkbox, Divider, FormLabel, Input, InputAdornment, List, ListItem, Typography } from "@mui/material";
import ShadowInput from "../Components/ShadowInput";
import { styled, alpha } from '@mui/system';
import EmailIcon from '@mui/icons-material/Email';
import Alert from '@mui/material/Alert';
import Collapse from '@mui/material/Collapse';
import LocalOfferIcon from '@mui/icons-material/LocalOffer';
import LoadingButton from "../Components/LoadingButton"
import {CircularProgress} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import Tooltip from '@mui/material/Tooltip';
import PhotoCamera from '@mui/icons-material/PhotoCamera';
import Button from '@mui/material/Button';
import { ContrastInput, ContrastInputNoOutline, ContrastInputWrapper, DeleteButton, FormInput, TextButton, TkrButton, TkrButton2 } from '../Styles/InputStyles';
import TagsBar from "../Components/TagsBar";
import AdminsBar from "../Components/AdminBar";
import { useNavigate } from "react-router-dom";
import UploadButtons from "../Components/InputTest";
import CategorySelector from "../Components/CategorySelector";
import SpotifyLogo from "../Images/SpotifyLogo.svg"
import SpotifyPlayer from "../Components/SpotifyPlayer";

export const EventForm = styled("div")({
  display: "flex",
  justifyContent: "center",
  flexDirection: "column",
  paddingTop: "15px",
  paddingBottom: "15px",
  paddingLeft: "15px",
  paddingRight: "15px",
  margin: "auto",
  alignItems: "center",
  gap: "10px",
});

export default function CreateEvent({}) {
  const navigate = useNavigate()
  var utc = require('dayjs/plugin/utc')
  dayjs.extend(utc)
  // States
  const [start, setStartValue] = React.useState({
    start: dayjs("2014-08-18T21:11:54"),
    error: false,
  });

  const [end, setEndValue] = React.useState({
    end: dayjs("2014-08-18T21:11:54"),
    error: false,
    errorMsg: "",
  });

  const [eventName, setEventName] = React.useState({
    value: '',
    error: false,
    errorMsg: '',
  })

  const [description, setDescription] = React.useState({
    value: '',
    error: false
  })

  const [address, setAddress] = React.useState({
    value: '',
    error: false,
  })

  const [suburb, setSuburb] = React.useState({
    value: '',
    error: false,
  })

  const [postcode, setPostcode] = React.useState({
    value: '',
    error: false
  })

  const [state, setState] = React.useState({
    value: '',
    error: false
  })

  const [country, setCountry] = React.useState({
    value: '',
    error: false
  })

  const [newAdmin, setNewAdmin] = React.useState({
    email: '',
    error: false,
    errorMsg: '',
    responseState: false,
    response: '',
  });

  const [adminList, setAdminList] = React.useState([]);

  const [seatingList, setSeatingList] = React.useState([]);

  const [tags, setTags] = React.useState([])

  const [errorStatus, setErrorStatus] = React.useState(false)

  const [errorMsg, setErrorMsg] = React.useState('')

  const [adminLoading, setAdminLoading] = React.useState(false)

  const [newSection, setNewSection] = React.useState({
    section: '',
    availability: 0,
    ticket_price: 0,
    has_seats: false,
    error: false,
    errorMsg: '',
  });

  const [loading, setLoading] = React.useState(false)

  const [eventPicture, setEventPicture] = React.useState(null)

  const [toggleUpload, setToggleUpload] = React.useState(true)

  const [newAdmins, setNewAdmins] = React.useState([])

  const [categories, setCategories] = React.useState([])

  const [spotifyPlaylist, setSpotifyPlaylist] = React.useState('')
  const [spotifyError, setSpotifyError] = React.useState('')
  const [validURL, setValidURL] = React.useState(false)

  React.useEffect(()=> {
    if(!errorStatus) {
      setErrorStatus(false)
      setErrorMsg('')
    }
  }, [errorStatus])

  const handleStartChange = (newValue) => {
    setFieldInState("start", newValue, start, setStartValue)
    setFieldInState("error", false, start, setStartValue)
    if (end.end < start.start) {
      setFieldInState("error", true, end, setEndValue);
      setFieldInState(
        "errorMsg",
        "End date must be after start date",
        end,
        setEndValue
      );
    } else {
      setFieldInState("error", false, end, setEndValue);
      setFieldInState("errorMsg", "", end, setEndValue);
    }
  };

  // Handle disable from start date
  // function disableStartDate (date) {
  //   return (date < start.start)
  // }
  
  const handleEndChange = (newValue) => {
    setFieldInState("end", newValue, end, setEndValue);
    setFieldInState("error", true, end, setEndValue);
    if (end.end <= start) {
      setFieldInState("error", true, end, setEndValue);
      setFieldInState(
        "errorMsg",
        "End date must be after start date",
        end,
        setEndValue
      );
    } else {
      setFieldInState("error", false, end, setEndValue);
      setFieldInState("errorMsg", "", end, setEndValue);
    }
  };

  const handleSectionChange = (e, index) => {
    const list = [...seatingList];
    list[index].sectionName = e.target.value;
    setSeatingList(list);
  };

  const handleCapacityChange = (e, index) => {
    const { name, value } = e.target;
    const list = [...seatingList];
    list[index][name] = value;
    setSeatingList(list);
  };

  const handleNewAdmin = (e) => {
    setFieldInState('error', false, newAdmin, setNewAdmin)
    setFieldInState('errorMsg', '', newAdmin, setNewAdmin)
    setFieldInState('email', e.target.value, newAdmin, setNewAdmin)
  };
  
  // Handles spotify playlist input
  const handleSpotifyChange = (e) => {
    // empty string provided
    setSpotifyError('')
    setSpotifyPlaylist(e.target.value)
    const link = e.target.value
    try {
      // Check for valid url
      const url = new URL(link)
      
      // Check for valid spotify url
      if (!isValidSpotifyURL(e.target.value)) {
        setSpotifyError('Invalid playlist URL.')
        // setSpotifyPlaylist('')
        return
      }
      
      setSpotifyError('')
    } catch (e) {
      setSpotifyError('Invalid playlist URL.')
      return
    }
  }

  const addAdmin = async (e) => {
    e.stopPropagation();
    e.nativeEvent.stopImmediatePropagation();

    // Check form input
    if (newAdmin.email.length === 0 || !checkValidEmail(newAdmin.email)) {
      setFieldInState('error', true, newAdmin, setNewAdmin)
      setFieldInState('errorMsg', 'Please enter a valid email', newAdmin, setNewAdmin)
      return
    }

    setAdminLoading(true)
    // Check if you are adding yourself to an event or user already is in event
    try {
      const response = await apiFetch('GET',`/api/user/profile?auth_token=${getToken()}`)
      if (newAdmin.email === response.email) {
        setFieldInState('error', true, newAdmin, setNewAdmin)
        setFieldInState('errorMsg', 'Cannot add yourself as admin', newAdmin, setNewAdmin)
        setAdminLoading(false)
        return
      }
    } catch (e) {
      console.log(e)
    }

    // Check if user exists
    try {
      const response = await apiFetch('GET', `/api/user/search?email=${newAdmin.email}`, null)

      // Check if user already added as an admin
      if (adminList.includes(response.user_id)) {
        setFieldInState('email', '', newAdmin, setNewAdmin)
        setAdminLoading(false)
        return
      }
      const adminList_t = [...adminList];
      adminList_t.push(response.user_id);
      setAdminList(adminList_t);

      // Add new admin to new adminList
      const newAdmins_t = [...newAdmins];
      newAdmins_t.push(response.user_id);
      setNewAdmins(newAdmins_t);

      setFieldInState('email', '', newAdmin, setNewAdmin)
      setAdminLoading(false)
    } catch (error) {
      setFieldInState('error', true, newAdmin, setNewAdmin)
      setFieldInState('errorMsg', error.reason, newAdmin, setNewAdmin)
      setAdminLoading(false)
    }
  };

  const removeAdmin = (index) => {
    const admin_list = [...adminList];
    admin_list.splice(index, 1);
    setAdminList(admin_list);
  };

  const addSection = (e) => {
    const sectionList = [...seatingList];
    sectionList.push(newSection.stateCopy);
    sortSection(sectionList)
    setSeatingList(sectionList);
    setFieldInState('section', '', newSection, setNewSection)
    setFieldInState('availability', 0, newSection, setNewSection)
    setFieldInState('ticket_price', 0, newSection, setNewSection)
    setFieldInState('has_seats', false, newSection, setNewSection)
  };

  const removeSeating = (index) => {
    const list = [...seatingList];
    list.splice(index, 1);
    setSeatingList(list);
  };

  React.useEffect(() => {
    if (newAdmin.response) {
    }
  }, [newAdmin.response])

  const submitEvent = async (e) => {
    setLoading(true)
    // Check fields
    var error = false
    if (eventName.value.length === 0) {
      setFieldInState('error', true, eventName, setEventName)
      error = true
    }
    if (address.value.length === 0) {
      setFieldInState('error', true, address, setAddress)
      error = true
    }
    if (suburb.value.length === 0) {
      setFieldInState('error', true, suburb, setSuburb)
      error = true
    }
    if (postcode.value.length === 0) {
      setFieldInState('error', true, postcode, setPostcode)
      error = true
    } 
    if (state.value.length === 0) {
      setFieldInState('error', true, state, setState)
      error = true
    }
    if (country.value.length === 0) {
      setFieldInState('error', true, country, setCountry)
      error = true
    }
    if (description.value.length === 0) {
      setFieldInState('error', true, description, setDescription)
      error = true
    }

    if (error) {
      setErrorStatus(true)
      setErrorMsg('Please fill in required fields')
      setLoading(false)
      return
    }

    // Check seating allocations
    if (seatingList.length === 0) {
      setErrorStatus(true)
      setFieldInState('error', true, newSection, setNewSection)
      setErrorMsg('Please allocate seating')
      setLoading(false)
      return
    }
    

    // Check if a valid street address
    const streetAddress = address.value.split(' ')
    if (streetAddress.length !== 3) {
      setFieldInState('error', true, address, setAddress)
      setErrorStatus(true)
      setErrorMsg('Please enter a valid street address. "{Street No} {Street Name}"')
      setLoading(false)
      return
    }
    if (isNaN(parseInt(streetAddress[0]))) {
      setFieldInState('error', true, address, setAddress)
      setErrorStatus(true)
      setErrorMsg('Please enter a valid street number. "{Street No} {Street Name}"')
      setLoading(false)
      return
    }

    if (end.end <= start.start) {
      setFieldInState("error", true, end, setEndValue);
      setFieldInState(
        "errorMsg",
        "End date must be after start date",
        end,
        setEndValue
      );
      setErrorMsg('End date must be after start date')
      setLoading(false)
      return
    }

    const locationBody = {
      street_no: +streetAddress[0],
      street_name: streetAddress[1] + ' ' + streetAddress[2],
      suburb: suburb.value,
      unitNo: '',
      postcode: postcode.value,
      state: state.value,
      country: country.value,
      longitude: '',
      latitude: ''
    };

    const start_date_t = dayjs(start.start).utc().format()
    const end_date_t = dayjs(end.end).utc().format()

    const body = {
      auth_token: getToken(),
      event_name: eventName.value,
      location: locationBody,
      start_date: start_date_t,
      end_date: end_date_t,
      description: description.value,
      seating_details: seatingList,
      categories: categories,
      tags: tags,
      admins: adminList,
      picture: eventPicture,
      spotify_playlist: isValidSpotifyURL(spotifyPlaylist) ? spotifyPlaylist : ""
    };

    try {
      const response = await apiFetch('POST', '/api/event/create', body)
      // Navigate to event
      navigate(`/view_event/${response.event_id}`)
      setLoading(false)
    } catch (e) {
      setErrorStatus(true)
      setLoading(false)
      setErrorMsg(e.reason)
    }
  };

  return (
    <BackdropNoBG sx={{boxShadow: 5}}>
      <Header />
      <Box
        sx={{
          minHeight: 600,
          maxWidth: 1500,
          marginLeft: "auto",
          marginRight: "auto",
          width: "95%",
          backgroundColor: "#FFFFFF",
          marginTop: "50px",
          borderRadius: "15px",
          paddingBottom: 5,
          paddingTop: 1,
          boxShadow: 5,
        }}
      >
        <H3 sx={{ fontSize: "30px" }}>Create Event</H3>
        <div>
          <EventForm>
            <Grid
              container
              spacing={2}
              sx={{
                marginLeft: 5,
                marginRight: 5,
                maxWidth: "1200px",
                width: "100%",
              }}
            >
              <Grid item xs={12}>
                <ContrastInputWrapper>
                  <CentredBox
                    sx={{
                      height: 200,
                      borderRadius: 5,
                    }}
                    onMouseOver={() => {
                      setToggleUpload(false);
                    }}
                    onMouseOut={() => {
                      setToggleUpload(true);
                    }}
                  > 
                    {!toggleUpload
                      ? <Button 
                          sx={{
                            backgroundColor: "#92C5DD",
                            "&:hover": {
                              backgroundColor: "#73B5D3"
                            },
                            color: 'white'
                          }}
                          variant="contained"
                          component="label"
                          startIcon={<PhotoCamera/>}
                        >
                          Upload Event Photo
                          <input
                            hidden
                            accept="image/*"
                            multiple
                            type="file" 
                            onChange={async (e) => {
                              const image = await fileToDataUrl(e.target.files[0])
                              setEventPicture(image)
                            }}
                          />
                        </Button>
                      : <>
                          {(eventPicture === null)
                            ? <h3>
                                Event cover photo
                              </h3>
                            : <UploadPhoto src={eventPicture}/>
                          }
                        </>                      
                    }
                  </CentredBox>
                </ContrastInputWrapper>
              </Grid>
              <Grid item xs={6}>
                <h3> Event Details </h3>
                <Grid container spacing={2}>
                  <Grid item xs={12}>
                    <ShadowInput
                      sx={{
                        fontWeight: 'bold',
                        '.MuiOutlinedInput-notchedOutline': {
                          borderColor: eventName.error ? "red" : "rgba(0,0,0,0)"
                        },
                      }}
                      state={eventName}
                      setState={setEventName}
                      defaultValue={eventName.value}
                      field='value'
                      placeholder="Event Name"
                      setError={setErrorStatus}
                    />
                  </Grid>
                  <Grid item xs={8}>
                    <ShadowInput 
                      state={address}
                      sx={{
                        '.MuiOutlinedInput-notchedOutline': {
                          borderColor: address.error ? "red" : "rgba(0,0,0,0)"
                        },
                      }}
                      setState={setAddress}
                      defaultValue={address.value}
                      field='value'
                      placeholder="Street Address"
                      setError={setErrorStatus}
                    />
                  </Grid>
                  <Grid item xs={4}>
                    <ShadowInput 
                      state={suburb}
                      sx={{
                        '.MuiOutlinedInput-notchedOutline': {
                          borderColor: suburb.error ? "red" : "rgba(0,0,0,0)"
                        },
                      }}
                      setState={setSuburb}
                      defaultValue={suburb.value}
                      field='value'
                      placeholder="Suburb"
                      setError={setErrorStatus}
                    />
                  </Grid>
                  <Grid item xs={3}>
                    <ShadowInput 
                      state={postcode} 
                      setState={setPostcode} 
                      sx={{
                        '.MuiOutlinedInput-notchedOutline': {
                          borderColor: postcode.error ? "red" : "rgba(0,0,0,0)"
                        },
                      }}
                      defaultValue={postcode.value} 
                      field='value' 
                      placeholder="Postcode"
                      setError={setErrorStatus}
                    />
                  </Grid>
                  <Grid item xs={4}>
                  <ShadowInput 
                      state={state} 
                      setState={setState} 
                      sx={{
                        '.MuiOutlinedInput-notchedOutline': {
                          borderColor: state.error ? "red" : "rgba(0,0,0,0)"
                        },
                      }}
                      defaultValue={state.value} 
                      field='value' 
                      placeholder="State"
                      setError={setErrorStatus}
                    />
                  </Grid>
                  <Grid item xs={5}>
                    <ShadowInput 
                      state={country} 
                      setState={setCountry} 
                      sx={{
                        '.MuiOutlinedInput-notchedOutline': {
                          borderColor: country.error ? "red" : "rgba(0,0,0,0)"
                        },
                      }}
                      defaultValue={state.value} 
                      field='value' 
                      placeholder="Country"
                      setError={setErrorStatus}
                    />
                  </Grid>
                  <LocalizationProvider dateAdapter={AdapterMoment}>
                    <Grid item xs={6}>
                      <FormControl fullWidth={false}>
                        <FormLabel sx={{"&.Mui-focused": {color: 'rgba(0, 0, 0, 0.6) '}, color: start.error ? "red" : "#999999"}}>Start Time</FormLabel>
                        <ContrastInputWrapper>
                          <DateTimePicker
                            value={start.start}
                            onChange={handleStartChange}
                            inputFormat="DD/MM/YYYY HH:mm"
                            renderInput={(params) => <TextField {...params} />}
                            disablePast = {true}
                            sx={{
                              '.MuiOutlinedInput-notchedOutline': {
                                borderColor: country.error ? "red" : "rgba(0,0,0,0)"
                              },
                            }}
                          />
                        </ContrastInputWrapper>
                      </FormControl>
                    </Grid>
                    <Grid item xs={6}>
                      <FormControl fullWidth={false}>
                        <FormLabel sx={{"&.Mui-focused": {color: 'rgba(0, 0, 0, 0.6) ',}, color: end.error ? "red" : "#999999"}}>End Time</FormLabel>
                        <ContrastInputWrapper>
                          <DateTimePicker
                            value={end.end}
                            onChange={handleEndChange}
                            inputFormat="DD/MM/YYYY HH:mm"
                            renderInput={(params) => <TextField {...params} />}
                            disablePast = {true}
                          />
                        </ContrastInputWrapper>
                        <FormHelperText>{end.errorMsg}</FormHelperText>
                      </FormControl>
                    </Grid>
                  </LocalizationProvider>

                  <Grid item xs={8}>
                    <FormControl fullWidth={true}>
                      <ContrastInputWrapper>
                        <ContrastInput 
                          value = {newAdmin.email}
                          placeholder="New Admin" 
                          startAdornment={<CentredBox sx={{pr: 1}}><EmailIcon sx={{color: "rgba(0,0,0,0.45)"}}/></CentredBox>} 
                          fullWidth 
                          onChange={handleNewAdmin}
                          sx={{
                            '.MuiOutlinedInput-notchedOutline': {
                              borderColor: newAdmin.error ? "red" : "rgba(0,0,0,0)"
                            },
                          }}
                        />
                      </ContrastInputWrapper>
                      <FormHelperText>{newAdmin.errorMsg}</FormHelperText>
                    </FormControl>
                  </Grid>
                  <Grid item xs={4}>
                    <CentredBox sx={{position: 'relative'}}>
                      <TkrButton
                        variant="contained"
                        disabled={(adminLoading || (newAdmin.email.length <= 0))}
                        sx={{mt: 1, fontSize: 15}}
                        onClick={addAdmin}
                        startIcon={<AddIcon/>}
                      >
                        Add Admin
                      </TkrButton>
                      {adminLoading && (
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
                  </Grid>
                  <Grid item xs={7}>
                    <AdminsBar editable={true} adminsList={adminList} newAdmins={newAdmins} removeAdmin={removeAdmin}/>
                  </Grid>
                  <Grid item xs={5}/>
                  <Grid item xs={12}>
                    <ContrastInputWrapper>
                      <ContrastInput
                        multiline
                        placeholder={'Enter a description'}
                        rows={4}
                        defaultValue={description.value}
                        fullWidth onChange={(e) => {
                          setFieldInState('value', e.target.value, description, setDescription)
                          setFieldInState('error', false, description, setDescription)
                          setErrorStatus(false)
                        }}
                        sx={{
                          '.MuiOutlinedInput-notchedOutline': {
                            borderColor: description.error ? "red" : "rgba(0,0,0,0)"
                          },
                        }}
                      />
                    </ContrastInputWrapper>
                  </Grid>
                </Grid>
              </Grid>
              <Grid item xs={1}></Grid>
              <Grid item xs={5}>
                <Box>
                  <h3> Ticket Allocations </h3>
                  <Grid container spacing={2}>
                    <Grid item xs={3}>
                      <Typography sx={{fontWeight: 'bold'}}>
                        Section
                      </Typography>
                      <Divider/>
                    </Grid>
                    <Grid item xs={3}>
                      <Typography sx={{fontWeight: 'bold'}}>
                        Availability
                      </Typography>
                      <Divider/>
                    </Grid>
                    <Grid item xs={3}>
                      <Typography sx={{fontWeight: 'bold'}}>
                        Cost
                      </Typography>
                      <Divider/>
                    </Grid>
                    <Grid item xs={2}>
                      <Typography sx={{fontWeight: 'bold'}}>
                        Seating
                      </Typography>
                      <Divider/>
                    </Grid>
                    {seatingList.map((value, index) => {
                      return (
                        <Grid item key={index} sx={{width: '100%'}}>
                          <ContrastInputWrapper>
                            <Grid container spacing={1}>
                              <Grid item xs={3}>
                                <Box sx={{display: 'flex', alignItems:'center', height: '100%'}}>
                                  <Typography
                                    sx={{
                                      fontWeight: 'bold',
                                    }}
                                  >
                                    {value.section}
                                  </Typography>
                                </Box>
                              </Grid>
                              <Grid item xs={3}>
                                <Box sx={{display: 'flex', alignItems:'center', height: '100%', pl: 2}}>
                                  <Typography
                                    sx={{
                                      fontWeight: 'bold',
                                    }}
                                  >
                                    {value.availability}
                                  </Typography>
                                </Box>
                              </Grid>
                              <Grid item xs={3}>
                                <Box sx={{display: 'flex', alignItems:'center', height: '100%', pl: 2}}>
                                  <Typography
                                    sx={{
                                      fontWeight: 'bold',
                                    }}
                                  >
                                    ${value.ticket_price}
                                  </Typography>
                                </Box>
                              </Grid>
                              <Grid item xs={2}>
                                <Box sx={{height: "100%", width: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center'}}>
                                  <Checkbox disabled checked={value.has_seats}/>
                                </Box>
                              </Grid>
                              <Grid item xs={1}>
                                <Box sx={{height: "100%", width: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center'}}>
                                  <IconButton
                                    edge="end"
                                    aria-label="delete"
                                    onClick={() => removeSeating(index)}
                                    sx={{marginRight: 0}}
                                  >
                                    <DeleteIcon />
                                  </IconButton>
                                </Box>
                              </Grid>
                            </Grid>
                          </ContrastInputWrapper>
                        </Grid>
                      );
                    })}
                  </Grid>
                  <Box sx={{marginRight: 4, width: '100%'}}>
                    <Grid container spacing={1}>
                      <Grid item xs={3}>
                        <ContrastInputWrapper>
                          <ContrastInput
                            placeholder={'Section'}
                            fullWidth 
                            onChange={(e) => {
                              setFieldInState('section', e.target.value, newSection, setNewSection)
                              setFieldInState('error', false, newSection, setNewSection)
                              setErrorStatus(false)
                            }}
                            sx={{
                              '.MuiOutlinedInput-notchedOutline': {
                                borderColor: newSection.error ? "red" : "rgba(0,0,0,0)"
                              },
                            }}
                            value = {newSection.section}
                          />
                        </ContrastInputWrapper>
                      </Grid>
                      <Grid item xs={3}>
                        <ContrastInputWrapper>
                          <ContrastInput 
                            type="number"
                            placeholder="Spots"
                            fullWidth 
                            onChange={(e) => {
                              const val = e.target.value
                              if (val < 0) {
                                setFieldInState('availability', 0, newSection, setNewSection)
                              } else {
                                setFieldInState('availability', val, newSection, setNewSection)
                              } 
                              setFieldInState('error', false, newSection, setNewSection)
                              setErrorStatus(false)
                            }}
                            sx={{
                              '.MuiOutlinedInput-notchedOutline': {
                                borderColor: newSection.error ? "red" : "rgba(0,0,0,0)"
                              },
                            }}
                            value = {newSection.availability}
                          />
                        </ContrastInputWrapper>
                      </Grid>
                      <Grid item xs={3}>
                        <ContrastInputWrapper>
                          <ContrastInput 
                            type="number"
                            placeholder="Cost"
                            fullWidth 
                            startAdornment={<InputAdornment position="start">$</InputAdornment>}
                            onChange={(e) => {
                              const val = e.target.value
                              if (val < 0) {
                                setFieldInState('ticket_price', 0, newSection, setNewSection)
                              } else {
                                setFieldInState('ticket_price', val, newSection, setNewSection)
                              } 
                              setFieldInState('error', false, newSection, setNewSection)
                              setErrorStatus(false)
                            }}
                            sx={{
                              '.MuiOutlinedInput-notchedOutline': {
                                borderColor: newSection.error ? "red" : "rgba(0,0,0,0)"
                              },
                            }}
                            value = {newSection.ticket_price}
                          />
                        </ContrastInputWrapper>
                      </Grid>
                      <Grid item xs={2}>
                        <ContrastInputWrapper sx={{ height: '100%'}}>
                          <CentredBox sx={{ height: '100%'}}>
                            <Checkbox
                              checked={newSection.has_seats}
                              onChange={(e) => {
                                setFieldInState('has_seats', e.target.checked, newSection, setNewSection)
                                setFieldInState('error', false, newSection, setNewSection)
                                setErrorStatus(false)
                              }}
                            />
                          </CentredBox>
                        </ContrastInputWrapper>
                      </Grid>
                      <Grid item xs={1}>
                        <ContrastInputWrapper 
                          sx={{
                            height: "100%",
                            width: '100%',
                            display: 'flex',
                            justifyContent: 'center',
                            alignItems: 'center',
                            backgroundColor: ((newSection.section.length === 0)|| (newSection.availability === 0)) ? "rgba(0, 0, 0, 0.08)" : alpha('#6A7B8A', 0.3)
                          }}
                        >
                          <IconButton
                            edge="end"
                            onClick={addSection}
                            sx={{marginRight: 0}}
                            disabled = {((newSection.section.length === 0) || (newSection.availability === 0))}
                          >
                            <AddIcon/>
                          </IconButton>
                        </ContrastInputWrapper>
                      </Grid>
                    </Grid>
                  </Box>
                </Box>
                <br/>
                <Box sx={{pt: 1, pb: 1}}>
                  <h3> Categories </h3>
                  <CategorySelector setSelectCategories={setCategories} selectCategories={categories} editable={true}/>
                </Box>
                <Box sx={{pt: 1, pb: 1}}>
                  <h3> Tags </h3>
                  <TagsBar tags={tags} setTags={setTags} editable={true}/>
                </Box>
                <Box sx={{pt: 1, pb: 1}}>
                  <h3> Spotify playlist </h3>
                  <FormControl sx={{width: "100%"}}>
                    <ContrastInputWrapper>
                      <ContrastInputNoOutline 
                        fullWidth
                        startAdornment={
                          <InputAdornment position="start">
                            <Avatar sx={{ width: 24, height: 24, bgcolor: 'rgba(0,0,0,0)'}} alt='SpotifyLogo' src={SpotifyLogo}/>
                          </InputAdornment>
                        }
                        onChange={handleSpotifyChange}
                        placeholder="Add Spotify playlist"
                      />
                    </ContrastInputWrapper>
                    <FormHelperText>{spotifyError}</FormHelperText>
                  </FormControl>
                  
                  {(isValidSpotifyURL(spotifyPlaylist))
                    ? <Box sx={{width: '100%', height: 500, borderRadius: 8, pt: 2}}>
                        <SpotifyPlayer link={spotifyPlaylist} setValidURL={setValidURL} editable={true}/>
                      </Box>
                    : <></>
                  }
                  
                </Box>
              </Grid>
            </Grid>
          </EventForm>
          <FormInput>
            <CentredBox sx={{position: 'relative'}}>
              <TkrButton
                variant="contained"
                onClick={submitEvent}
                disabled={loading}
              >
                Create Event
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
            <Collapse in={errorStatus}>
              <Alert severity="error">{errorMsg}.</Alert>
            </Collapse>
          </FormInput>
        </div>
      </Box>
    </BackdropNoBG>  
  );
}
