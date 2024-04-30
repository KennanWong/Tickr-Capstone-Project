import React from "react";

import Header from "../Components/Header";
import { BackdropNoBG, BackdropNoBG_VH, CentredBox, ScrollableBox, UploadPhoto } from "../Styles/HelperStyles";
import dayjs from "dayjs";
import Grid from "@mui/material/Unstable_Grid2";
import { Box, Chip, Divider, FormGroup, FormHelperText, IconButton, Tooltip, Typography } from "@mui/material";
import { styled, alpha } from '@mui/system';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import AvatarGroup from '@mui/material/AvatarGroup';
import LinearProgress from '@mui/material/LinearProgress';
import TagsBar from "../Components/TagsBar";
import UserAvatar from "../Components/UserAvatar";
import AdminsBar from "../Components/AdminBar";
import { useNavigate, useParams } from "react-router-dom";
import { apiFetch, checkIfUser, getEventData, getTicketIds, getToken, getUserData, isValidSpotifyURL, loggedIn, search } from "../Helpers";
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import EditIcon from '@mui/icons-material/Edit';
import { ContrastInputNoOutline, ContrastInputWrapper, HoverChipSelected, TextButton2, TkrButton, TkrButton2 } from "../Styles/InputStyles";
import EventReview from "../Components/EventReview";
import SendIcon from '@mui/icons-material/Send';
import AddIcon from '@mui/icons-material/Add';
import SpotifyPlayer from "../Components/SpotifyPlayer";
import CategorySelector from "../Components/CategorySelector";
import { Section, SectionHeading } from "../Components/RecommendedEvents";
import RecommendedEventsBar from "../Components/RecommendedEventsBar";

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

export default function ViewEvent({}) {
  var calendar = require('dayjs/plugin/calendar')
  var utc = require('dayjs/plugin/utc')
  dayjs.extend(calendar)
  dayjs.extend(utc)
  const params = useParams()
  const navigate = useNavigate()

  const [editable, setEditable] = React.useState(false)
  const [eventOver, setEventOver] = React.useState(false)
  const [soldOut, setSoldOut] = React.useState(false)
  const [isAttendee, setIsAttendee] = React.useState(false)
  const [ticketIds, setTicketIds] = React.useState([])
  const [announcement, setAnnouncement] = React.useState('')
  const [following, setFollowing] = React.useState(false)

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
    categories: [],
    admins: [],
    picture: "",
    host_id: ''
  })

  // Handle new announced
  const handleAnnouncement = (e) => {
    setAnnouncement(e.target.value)
  }

  // Post announcement
  const postAnnouncement = async (e) => {
    try {
      const body = {
        event_id: params.event_id,
        auth_token: getToken(),
        announcement: announcement
      }
      const response = await apiFetch('POST', '/api/event/announce', body)
      setAnnouncement('')
    } catch (e) {
      
    }
  }

  // Initial load
  React.useEffect(()=> {
    getEventData(params.event_id, setEvent, getToken())

    // Check if we have the host_id is provided to check if event data has been captured
    if (event.host_id !== '') {
      if (dayjs() > dayjs(event.end_date)) {
        setEventOver(true)
      }

      // Check if the user is the host
      checkIfUser(event.host_id, setEditable)

      // Check if the user is in event.admins
      if (!editable) {
        for (const i in event.admins) {
          checkIfUser(event.admins[i], setEditable)
        }
      }
    }

    // If user is logged in get tickets for event if any and get following status
    if (loggedIn()) {
      getTicketIds(params.event_id, setTicketIds)
      getFollowing()
    }


  },[event.host_id])


  // Check if user is following this event
  const getFollowing = async() => {
    try {
      const body = {
        auth_token: getToken(),
        event_id: params.event_id
      }
      const searchParams = new URLSearchParams(body)
      const response = await apiFetch('GET', `/api/event/notifications?${searchParams}`)
      setFollowing(response.notifications)


    } catch (e)  {
      console.log(e)
    }
  }

  const toggleFollowing = async (e) => {
    setFollowing(!following)
    try {
      const body = {
        auth_token: getToken(),
        event_id: params.event_id,
        notifications: !following
      }
      const response = await apiFetch('PUT', '/api/event/notifications/update', body)
    } catch (e) {
      console.log(e)
    }
  }

  // Check if event is sold out
  React.useEffect(() => {
    var isSoldOut = true
    try {
      if (event.seating_details !== null) {
        event.seating_details.forEach(function (section) {
          if (section.available_seats > 0 ) {
            isSoldOut = false
            return
          }
        })
        setSoldOut(isSoldOut)
      }
    } catch (e) {
    }
  }, [event.seating_details])

  // Set if user has tickets for this event
  React.useEffect(() => {
    setIsAttendee(ticketIds.length > 0)
  },[ticketIds])

  const goEdit = (e) => {
    e.stopPropagation();
    e.nativeEvent.stopImmediatePropagation();
    navigate(`/edit_event/${params.event_id}`)
  }

  return (
    <BackdropNoBG_VH>
      <Header />
      <ScrollableBox sx={{height: 'calc(100vh - 70px)'}}>
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
            :<div>
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
                    <CentredBox
                      sx={{
                        height: 200,
                        backgroundColor: alpha('#6A7B8A', 0.3),
                        borderRadius: 5,
                      }}
                    >
                      {(event.picture === '')
                        ? <h3>
                            Event cover photo
                          </h3>
                        : <UploadPhoto src={event.picture}/>
                      }
                    </CentredBox>
                  </Grid>
                  <Grid item xs={6}>
                    <Grid container spacing={0}>
                      <Grid item xs={12}>
                        {eventOver
                          ? <Box sx={{display: 'flex', gap: 5}}>
                              <Typography
                                sx={{
                                  fontSize: 40,
                                  fontWeight: 'bold',
                                }}
                              >
                                {event.event_name}
                              </Typography>
                              <CentredBox>
                                <Chip sx={{color: alpha('#6A7B8A', 0.7)}} label="Event Passed"/>
                              </CentredBox>
                            </Box>
                          : <Grid container spacing={2}>
                              <Grid item xs={10}>
                                <Box sx={{display: 'flex', gap: 2, alignItems: 'center'}}>
                                  <Typography
                                    sx={{
                                      fontSize: 40,
                                      fontWeight: 'bold',
                                    }}
                                  >
                                    {event.event_name}
                                  </Typography>
                                  {event.published
                                    ? <CentredBox>
                                        {(!editable && isAttendee)
                                          ? <Tooltip title="Receive email notifications about event changes and announcements.">
                                              {following
                                                ? <HoverChipSelected sx={{width: 90}} clickable onClick={toggleFollowing} label="Following"/>
                                                : <Chip clickable onClick={toggleFollowing} variant="outlined" color='secondary' sx={{ color: "#AE759F", width: 90}} icon={<AddIcon sx={{ color: "#AE759F" }}/>} label="Follow"/>
                                              }
                                            </Tooltip>
                                          : <></>
                                        }
                                      </CentredBox>
                                    : <CentredBox>
                                        <Tooltip title="Edit event to publish event.">
                                          <Chip sx={{color: alpha('#6A7B8A', 0.7)}} label="Not Published"/>
                                        </Tooltip>
                                      </CentredBox>
                                  }
                                </Box>
                              </Grid>
                              <Grid item xs={2}>
                                {(editable)
                                  ? <Box sx={{display: 'flex', height: '100%', alignItems: 'center', justifyContent: 'flex-end'}}>
                                      <Tooltip title="Edit Event">
                                        <IconButton onClick={goEdit}>
                                          <EditIcon/>
                                        </IconButton>
                                      </Tooltip>
                                    </Box>
                                  : <></>
                                }
                              </Grid>
                            </Grid> 
                        }
                      </Grid>
                      <Grid item xs={12}>
                        <Grid container>
                          <Grid item xs={1} sx={{display: 'flex', alignItems: 'center'}}>
                            <CentredBox sx={{gap: '5px', justifyContent: 'flex-start'}}>
                              <CalendarMonthIcon sx={{color: '#AE759F'}}/>
                            </CentredBox>
                          </Grid>
                          <Grid item xs={11}>
                            <Typography
                              sx={{
                                fontSize: 15,
                                color: "#AE759F",
                              }}
                            >
                              {dayjs(event.start_date).local().format('LLLL')} - {dayjs(event.end_date).local().format('LLLL')}
                            </Typography>
                          </Grid>
                        </Grid>
                      </Grid>
                      <Grid item xs={12}>
                        <Grid container>
                          <Grid item xs={1} sx={{display: 'flex', alignItems: 'center'}}>
                            <CentredBox sx={{gap: '5px', justifyContent: 'flex-start'}}>
                              <LocationOnIcon sx={{color: '#000000'}}/>
                            </CentredBox>
                          </Grid>
                          <Grid item xs={11}>
                            <Typography sx={{fontSize: 18}}>
                              {event.location.street_no} {event.location.street_name}, {event.location.suburb}, {event.location.postcode}, {event.location.state}, {event.location.country}
                            </Typography>
                          </Grid>
                        </Grid>
                      </Grid>
                      <Grid item xs={12}>
                        <br/>
                        <Typography
                          sx={{
                            color: "#999999",
                            fontSize: 20
                          }}
                        >
                          About this event
                        </Typography>
                        <Divider/>
                        <Typography sx={{pt: 2, fontSize: 18}}>
                          {event.description}
                        </Typography>
                      </Grid>
                      <Grid item xs={12}>
                        <br/>
                        <Box>
                          <Typography
                            sx={{
                              color: "#999999",
                              textDecoration: 'underline',
                              fontSize: 18
                            }}
                          >
                            Host and event Admins
                          </Typography>
                          <AvatarGroup max={5} sx={{flexDirection: 'row', pt:2}}>
                            {event.admins.map((value, key) => {
                              return (
                                <UserAvatar key={key} userId={value} size={35}/>
                              );
                            })}
                            <UserAvatar userId={event.host_id} size={35} host={true}/>
                          </AvatarGroup>
                        </Box>
                        <br/>
                      </Grid>
                      <Grid item xs={12}>
                        <br/>
                        {(event.categories.length > 0)
                          ? <Box>
                              <Typography
                                sx={{
                                  color: "#999999",
                                  textDecoration: 'underline',
                                  fontSize: 18
                                }}
                              >
                                Categories
                              </Typography>
                              <Box sx={{display: 'flex', gap: 1}}>
                                {event.categories.map((category, key) => {
                                  return (
                                    <TextButton2 key={key} onClick={(e) => {search('categories', [category], navigate)}}>
                                      {category}
                                    </TextButton2>
                                  )
                                })}
                              </Box>
                            </Box>
                          : <></>
                        }         
                      </Grid>
                      <Grid item xs={12}>
                        <br/>
                        {(event.tags.length > 0)
                          ? <Box>
                              <Typography
                                sx={{
                                  color: "#999999",
                                  textDecoration: 'underline',
                                  fontSize: 18
                                }}
                              >
                                Tags
                              </Typography>
                              <TagsBar tags={event.tags} editable={false} navigate={navigate}/>
                            </Box>
                          : <></>
                        }         
                      </Grid>
                    </Grid>
                  </Grid>
                  <Grid item xs={1}></Grid>
                  <Grid item xs={5}>
                    {eventOver
                      ? <></>
                      : <Box>
                          <Typography
                            sx={{
                              fontWeight: 'bold',
                              fontSize: 35,
                              pt: 2,
                            }}
                          >
                            Tickets
                          </Typography>
                          {(!soldOut)
                            ? <TableContainer>
                                <Table stickyHeader sx={{maxHeight: 300}}>
                                  <TableHead>
                                    <TableRow>
                                      <TableCell sx={{fontWeight: 'bold', fontSize: 20}}>Section</TableCell>
                                      <TableCell sx={{fontWeight: 'bold', fontSize: 20}} align="center">Availability</TableCell>
                                      <TableCell sx={{fontWeight: 'bold', fontSize: 20}} align="center">Cost</TableCell>
                                    </TableRow>
                                  </TableHead>
                                  <TableBody>
                                    {event.seating_details.map((section, key) => {
                                      if (section.available_seats > 0) {
                                        return (
                                          <TableRow key={key}>
                                            <TableCell>{section.section}</TableCell>
                                            <TableCell align="center">{section.available_seats}</TableCell>
                                            <TableCell align="center">${section.ticket_price}</TableCell>
                                          </TableRow>
                                        )
                                      }
                                    })}
                                  </TableBody>
                                </Table>
                              </TableContainer>
                            : <>
                              </>
                          }
                          <br/>
                          <>
                            {event.published
                              ? <>
                                  {isAttendee
                                    ? <Grid container>
                                        <Grid item xs={6}>
                                          <TkrButton2 sx={{fontSize: '19px', width: '100%'}} onClick={() => navigate(`/view_tickets/${params.event_id}`)}>
                                            View Tickets
                                          </TkrButton2>
                                        </Grid>
                                        <Divider orientation="vertical" flexItem/>
                                        <Grid item xs>
                                          {!soldOut
                                            ? <TkrButton sx={{fontSize: '19px', width: '100%'}} onClick={() => navigate(`/purchase_ticket/${params.event_id}`)}>
                                                Purchase tickets
                                              </TkrButton>
                                            : <TkrButton  disabled sx={{fontSize: '19px', width: '100%', backgroundColor: '#EEEEEE'}}>
                                                Sold Out
                                              </TkrButton>
                                          }
                                          
                                        </Grid>
                                      </Grid> 
                                    : <CentredBox>
                                        {(!soldOut)
                                          ? <>  
                                              {loggedIn()
                                                ? <TkrButton sx={{fontSize: '19px', width: '100%'}} onClick={() => navigate(`/purchase_ticket/${params.event_id}`)}>
                                                    Purchase tickets
                                                  </TkrButton>
                                                : <FormGroup sx={{width: '100%'}}>
                                                    <TkrButton disabled sx={{fontSize: '19px', width: '100%'}} >
                                                      Purchase tickets
                                                    </TkrButton>
                                                    <FormHelperText><Typography component={'span'} sx={{textAlign: 'center'}}>Log in to purchase</Typography></FormHelperText>
                                                  </FormGroup>
                                              }
                                            </>
                                          : <TkrButton  disabled sx={{fontSize: '19px', width: '100%', backgroundColor: '#EEEEEE'}}>
                                              Sold Out
                                            </TkrButton>
                                        }
                                      </CentredBox>
                                  }
                                </>
                              : <></>
                            }
                          </>
                        </Box>
                    }
                    <br/>
                    <br/>
                    {(editable && event.published)
                      ? <Box>
                          <Typography
                            sx={{
                              color: "#999999"
                            }}
                          >
                            Make an announcement
                          </Typography>
                          <Divider/>
                          <ContrastInputWrapper sx={{mt: 2}}>
                            <ContrastInputNoOutline
                              value={announcement}
                              multiline
                              placeholder={'Enter an announcement'}
                              rows={4}
                              fullWidth
                              onChange={handleAnnouncement}
                            >
                            </ContrastInputNoOutline>
                          </ContrastInputWrapper>
                          <TkrButton2 sx={{mt: '3px', width: '100%'}}
                            endIcon={
                              <SendIcon/>
                            }
                            disabled={announcement.length === 0}
                            onClick={postAnnouncement}
                          >
                            Post
                          </TkrButton2>
                        </Box>
                      : <></>
                    }
                    {(isValidSpotifyURL(event.spotify_playlist)) 
                      ? <Box sx={{width: '100%', pt: 2, height: 500, borderRadius: 8}}>
                          <Typography
                            sx={{
                              color: "#999999",
                              fontSize: 18,
                              textDecoration: 'underline'
                            }}
                          >
                            Event playlist
                          </Typography>
                          <SpotifyPlayer link={event.spotify_playlist}/>
                        </Box>
                      : <></>
                    }
                  </Grid>
                  <Grid item xs={12}>
                    <br/>
                    <br/>
                    <Box sx={{width: '100%', pb: 5}}>
                      <Section>
                        <SectionHeading sx={{fontSize: 30}}>
                          Similar Events
                        </SectionHeading>
                        {loggedIn()
                          ? <RecommendedEventsBar endpoint={'/api/user/recommendations/event'} additionalParams={{auth_token: getToken(), event_id: params.event_id}}/>
                          : <RecommendedEventsBar endpoint={'/api/recommendations/event'} additionalParams={{event_id: params.event_id}}/>
                        }
                      </Section>
                    </Box>
                  </Grid>
                </Grid>
              </EventForm>
              {eventOver
                ? <EventReview isAttendee={isAttendee} event_id={params.event_id} isHost={editable}/>
                : <>
                  </>
              }
              
            </div>
          }
        </Box>
      </ScrollableBox>
    </BackdropNoBG_VH>
  );
}
