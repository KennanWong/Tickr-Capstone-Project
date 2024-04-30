import { Divider, Grid, LinearProgress, Typography, Tooltip, IconButton, FormControl, FormHelperText, Collapse, Alert } from '@mui/material'
import { Box } from '@mui/system'
import React from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Header from '../Components/Header'
import StandardLogo from '../Components/StandardLogo'
import { apiFetch, getEventData, getToken, loggedIn, setFieldInState, setReservedTicketsLocal } from '../Helpers'
import { Backdrop, BackdropNoBG, CentredBox, UploadPhoto } from '../Styles/HelperStyles'
import { DeleteButton, TkrButton, ContrastInput, ContrastInputWrapper, TicketOption } from '../Styles/InputStyles'
import dayjs from "dayjs";
import ArrowBackIcon from '@mui/icons-material/ArrowBack';


import '../App.css';
import { EventForm } from './CreateEvent'

export default function AcceptInvite({}) {
  const params = useParams()
  const navigate = useNavigate()
  const inviteId = params.invite_id
  var calendar = require('dayjs/plugin/calendar')
  dayjs.extend(calendar)
  
  const [accepted, setAccepted] = React.useState(false)
  const [reserveId, setReserveId] = React.useState('')
  const [reserveDetails, setReserveDetails] = React.useState({
    section: 'A',
    seat_number: '10',
    price: 0,
    event_id: ''
  })
  const [event, setEvent] = React.useState({
    event_name: "",
    location: {
      street_no: "",
      street_name: "",
      postcode: "",
      state: "",
      country: ""
    },
    host_id: '',
    start_date: dayjs().toISOString(),
    end_date: dayjs().toISOString(),
    description: "",
    tags: [],
    admins: [],
    picture: "",
    host_id: 'k'
  })

  const [firstName, setFirstName] = React.useState({
    label: 'first name',
    value: '',
    error: false,
    errorMsg: ''
  })
  const [lastName, setLastName] = React.useState({
    label: 'last name',
    value: '',
    error: false,
    errorMsg: ''
  })
  const [email, setEmail] = React.useState({
    value: '',
    error: false,
    errorMsg: ''
  })

  const [error, setError] = React.useState(false)

  // Handles for state changes
  const handleFirstNameChange = (e) => {
    // Clear error
    setFieldInState('error', false, firstName, setFirstName)
    setFieldInState('errorMsg', '', firstName, setFirstName)

    setFieldInState('value', e.target.value, firstName, setFirstName)
  }

  const handleLastNameChange = (e) => {
    // Clear error
    setFieldInState('error', false, lastName, setLastName)
    setFieldInState('errorMsg', '', lastName, setLastName)

    setFieldInState('value', e.target.value, lastName, setLastName)
  }

  const handleEmailChange = (e) => {
    // Clear error
    setFieldInState('error', false, email, setEmail)
    setFieldInState('errorMsg', '', email, setEmail)

    setFieldInState('value', e.target.value, email, setEmail)
  }

  React.useEffect(() => {
    if (!loggedIn()) {
      navigate(`/invite/login/${inviteId}`)
    }
  },[])

  const getReserveDetails = async () => {
    try {
      const reserveDetailsBody = {
        reserve_id: reserveId
      }
      const reserveSearchParams = new URLSearchParams(reserveDetailsBody)
      const reserveResponse = await apiFetch('GET', `/api/reserve/details?${reserveSearchParams}`, null)
      setReserveDetails(reserveResponse)
      getEventData(reserveResponse.event_id, setEvent)
    } catch (e) {
      console.log(e)
    }
  }

  // If accepted, fetch reserve details
  React.useEffect(() => {
    if (accepted) {
      getReserveDetails()
    }
  }, [accepted])

  const handleAccept = async () => {
    const body = {
      auth_token: getToken(),
      invite_id: inviteId
    }

    try {
      const response = await apiFetch('POST', '/api/group/accept', body)
      setReserveId(response.reserve_id)
      setReservedTicketsLocal([response.reserve_id])
      setAccepted(true)
    } catch (e) {
      console.log(e)
      setError(true)
    }
  }

  const handleDecline = async () => {
    const body = {
      invite_id: inviteId
    }
    try {
      const response = await apiFetch('POST', '/api/group/deny')
      navigate('/')
    } catch (e) {
      console.log(e)
    }
  }

  const handleCheckout = async () => {
    const ticketDetails = [{
      first_name: firstName.value,
      last_name: lastName.value,
      email: email.value,
      request_id: reserveId
    }]

    const body = {
      auth_token: getToken(),
      ticket_details: ticketDetails,
      success_url: `http://localhost:3000/view_tickets/${reserveDetails.event_id}`,
      cancel_url: `http://localhost:3000/cancel_reservation`
    }

    try {
      const response = await apiFetch('POST', '/api/ticket/purchase', body)
      window.location.replace(response.redirect_url)
    } catch (error) {
      console.log(error)
    }
  }

  return (
    <Box>
      {!accepted
        ? <Backdrop>
            <StandardLogo/>
            <Box sx={{height: '100%', display: 'flex', alignItems: 'center'}}>
              <Box
                sx={{
                  width: 800,
                  marginLeft: "auto",
                  marginRight: "auto",
                  backgroundColor: "#F5F5F5",
                  marginTop: "50px",
                  borderRadius: 3,
                  paddingBottom: 5,
                  paddingTop: 1,
                  boxShadow: 3
                }}
              >
                {!error
                  ? <>
                      <Typography sx={{fontSize: 50, fontFamily: 'Segoe UI', textAlign: 'center'}}>
                        Accept invite to join group?
                      </Typography>
                      <Box sx={{pt: 5, pl: 30, pr: 30, display: 'flex', justifyContent: 'space-between'}}>
                        <DeleteButton onClick={handleDecline}>Decline</DeleteButton>
                        <TkrButton onClick={handleAccept}>Accept</TkrButton>
                      </Box>
                    </>
                  : <CentredBox sx={{height: '100%', alignItems: 'center', flexDirection: 'column', gap: 5}}>
                      <Typography sx={{fontSize: 50, fontFamily: 'Segoe UI', textAlign: 'center'}}>
                        Invite expired.
                      </Typography>
                      <TkrButton onClick={() => {navigate('/')}}>Back to Tickr</TkrButton>
                    </CentredBox>
                }
                
              </Box>
            </Box>
          </Backdrop> 
        : <BackdropNoBG>
            <Header/>
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
              }}
            >
              {(event.host_id === '')
                ? <Box sx={{height: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center', margin: '50px', paddingTop: '100px'}}> 
                    <Box sx={{ width: '90%', hieght: 50}}>
                      <LinearProgress color='secondary'/>
                    </Box>
                  </Box>
                : <EventForm>
                    <Grid container spacing={2}>
                      <Grid item xs={7}>
                        <Grid container>
                          <Grid item xs={1}>
                            <CentredBox sx={{height: '100%'}}>
                              <Tooltip title="Back to event">
                                <IconButton onClick={()=>{
                                  navigate(`/view_event/${reserveDetails.event_id}`) 
                                }}>
                                  <ArrowBackIcon/>
                                </IconButton>
                              </Tooltip>
                            </CentredBox>
                          </Grid>
                          <Grid item xs={10}>
                            <CentredBox sx={{flexDirection: 'column'}}>
                              <Typography sx={{fontWeight: 'bold', fontSize: 40, pt: 1}}>
                                {event.event_name}
                              </Typography>
                              <Typography
                                sx={{
                                  fontSize: 15,
                                  fontWeight: "regular",
                                  color: "#AE759F",
                                }}
                              >
                                {dayjs(event.start_date).format('lll')} - {dayjs(event.end_date).format('lll')}
                              </Typography>
                            </CentredBox>
                          </Grid>
                          <Grid item xs={1}>
                          </Grid>
                        </Grid>
                        <Divider/>
                        <br/> 
                        <Box sx={{display: 'flex', justifyContent: 'space-between', pb: 1, pl: 15}}>
                          <Typography sx={{fontSize: 30, fontWeight: 'bold', color: '#333333'}}>
                            Enter Ticket Details
                          </Typography>
                        </Box>
                        <CentredBox sx={{ml: 15, mr: 15, flexDirection: 'column', gap: 2}}>
                          <TicketOption
                            sx={{
                              widht: 70,
                              height: 70,
                              backgroundColor: "#AE759F",
                              color: '#FFFFFF' 
                            }} 
                          >
                            {reserveDetails.section[0]}{reserveDetails.seat_number}
                          </TicketOption>
                          <Grid container spacing={1}>
                            <Grid item xs={6}>
                              <FormControl sx={{width: '100%'}}>
                                <ContrastInputWrapper>
                                  <ContrastInput
                                    fullWidth
                                    placeholder="First Name"
                                    onChange={handleFirstNameChange}
                                    value={firstName.value}
                                    error={firstName.error}
                                  >
                                  </ContrastInput>
                                </ContrastInputWrapper>
                                <FormHelperText>{firstName.errorMsg}</FormHelperText>
                              </FormControl>
                            </Grid>
                            <Grid item xs={6}>
                              <FormControl sx={{width: '100%'}}>
                                <ContrastInputWrapper>
                                  <ContrastInput
                                    fullWidth
                                    placeholder="Last Name"
                                    onChange={handleLastNameChange}
                                    value={lastName.value}
                                    error={lastName.error}
                                    
                                  />
                                </ContrastInputWrapper>
                                <FormHelperText>{lastName.errorMsg}</FormHelperText>
                              </FormControl>
                            </Grid>
                            <Grid item xs={12}>
                              <FormControl sx={{width: '100%'}}>
                                <ContrastInputWrapper>
                                  <ContrastInput
                                    placeholder="Email"
                                    fullWidth
                                    onChange={handleEmailChange}
                                    value={email.value}
                                    error={email.error}
                                  >
                                  </ContrastInput>
                                </ContrastInputWrapper>
                                <FormHelperText>{email.errorMsg}</FormHelperText>
                              </FormControl>
                            </Grid>
                            <Grid item xs={12}>
                              <CentredBox sx={{flexDirection: 'column'}}>
                                <TkrButton sx={{width: 130, height: 50}} onClick={handleCheckout}> Checkout </TkrButton>
                                <Collapse sx={{pt: 1}} in={(firstName.error || lastName.error || email.error)}>
                                  <Alert severity="error">{firstName.error ? firstName.errorMsg : ''}{lastName.error ? lastName.errorMsg : ''}{email.error ? email.errorMsg : ''}</Alert>
                                </Collapse>
                              </CentredBox>
                            </Grid>
                          </Grid>
                        </CentredBox>
                      </Grid>
                      <Divider orientation="vertical" flexItem></Divider>
                      <Grid item xs>
                        <Box sx={{height: "100%"}}>
                          <CentredBox sx={{flexDirection: 'column', widht: '100%'}}>
                            {(event.picture !== '')
                              ? <UploadPhoto src={event.picture} sx={{width:'100%', height: 300}}/>
                              : <Box sx={{width: '100%', height: 300, borderRadius: 5, backgroundColor: '#EEEEEE'}}>
                                  <CentredBox sx={{height: '100%', alignItems: 'center'}}>
                                    <Typography sx={{fontWeight: 'bold', fontSize: 40, pt: 1, texAlign: 'center'}}>
                                      {event.event_name}
                                    </Typography>
                                  </CentredBox>
                                </Box>
                            }
                          </CentredBox>
                          <br/>
                          <Divider/>
                          <br/>
                          <Box sx={{backgroundColor: '#EEEEEE', display: 'flex', borderRadius: 2, p: 2}}>
                            <Box sx={{width: '100%'}}>
                              <Typography
                                sx={{textAlign: 'center', fontSize: 20, pb: 1}}
                              >
                                Order Summary
                              </Typography>
                              <Divider/>
                              <br/>
                              <Box>
                                <Grid container spacing={2} sx={{pl: 1, pr: 1}}>
                                  <Grid container spacing={2} sx={{pl: 1, pr: 1}}>
                                    <Grid item xs={9}>
                                      <Typography sx={{fontSize: 20}}>
                                        1 x {reserveDetails.section} - ${reserveDetails.price}
                                      </Typography>
                                      <Box sx={{display: 'flex', gap: 1, flexWrap: 'wrap'}}>
                                        <Typography sx={{color: 'rgba(0, 0, 0, 0.6)'}}>
                                          {reserveDetails.section[0]}{reserveDetails.seat_number}
                                        </Typography>
                                      </Box>
                                    </Grid>
                                    <Grid item xs={3}>
                                      <Typography sx={{fontSize: 20, textAlign: 'right'}}>
                                        {/* ${(section.ticket_price*section.quantity)} */}
                                      </Typography>
                                    </Grid>
                                  </Grid>
                                </Grid>
                                <br/>
                                <Divider/>
                                <br/>
                                <Grid container spacing={2} sx={{pl: 2, pr: 2}}>
                                  <Grid item xs={9}>
                                    <Typography sx={{fontSize: 20}}>
                                      Total: 
                                    </Typography>
                                  </Grid>
                                  <Grid item xs={3}>
                                    <Typography sx={{fontSize: 25, textAlign: 'right'}}>
                                      ${reserveDetails.price}
                                    </Typography>
                                  </Grid>
                                </Grid>
                              </Box>
                            </Box>
                          </Box>
                        </Box>
                      </Grid>
                    </Grid>
                  </EventForm>
              }
            </Box>
          </BackdropNoBG>
        }
    </Box>
  )
}