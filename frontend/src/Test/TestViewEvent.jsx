import React from "react";

import Header from "../Components/Header";
import { BackdropNoBG, CentredBox, ScrollableBox, UploadPhoto } from "../Styles/HelperStyles";
import dayjs from "dayjs";
import Grid from "@mui/material/Unstable_Grid2";
import { Box, Chip, Divider, IconButton, Tooltip, Typography } from "@mui/material";
import { styled, alpha } from '@mui/system';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import AvatarGroup from '@mui/material/AvatarGroup';
import LinearProgress from '@mui/material/LinearProgress';
import TagsBar from "../Components/TagsBar";
import UserAvatar from "../Components/UserAvatar";
import AdminsBar from "../Components/AdminBar";
import { useNavigate, useParams } from "react-router-dom";
import { apiFetch, checkIfUser, getEventData, getTicketIds, getToken, getUserData } from "../Helpers";
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import EditIcon from '@mui/icons-material/Edit';
import { TkrButton, TkrButton2 } from "../Styles/InputStyles";
import EventReview from "../Components/EventReview";

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

export default function TestViewEvent({}) {
  var calendar = require('dayjs/plugin/calendar')
  dayjs.extend(calendar)
  const params = useParams()
  const navigate = useNavigate()
  const [editable, setEditable] = React.useState(false)
  const [eventOver, setEventOver] = React.useState(false)
  const [soldOut, setSoldOut] = React.useState(false)
  const [isAttendee, setIsAttendee] = React.useState(false)
  const [ticketIds, setTicketIds] = React.useState([])

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
    host_id: ''
  })

  React.useEffect(()=> {
    getEventData(params.event_id, setEvent)
    // getUserData(`auth_token=${getToken()}`,setUserData)
    if (event.host_id !== '') {
      checkIfUser(event.host_id, setEditable)
      if (!editable) {
        for (const i in event.admins) {
          checkIfUser(event.admins[i], setEditable)
        }
      }
    }
    getTicketIds(params.event_id, setTicketIds)
  },[event.host_id])

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
    <BackdropNoBG>
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
                      <Box sx={{display: 'flex', justifyContent: 'space-between'}}>
                        <Typography
                          sx={{
                            fontSize: 40,
                            fontWeight: 'bold',
                          }}
                        >
                          {event.event_name}
                        </Typography>
                        {(editable)
                          ? <Tooltip title="Edit Event">
                              <IconButton onClick={goEdit}>
                                <EditIcon/>
                              </IconButton>
                            </Tooltip>
                          : <></>
                        }
                      </Box>
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
                              fontSize: 17,
                              color: "#AE759F",
                            }}
                          >
                            {dayjs(event.start_date).format('LLLL')} - {dayjs(event.end_date).format('LLLL')}
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
                          <Typography>
                            {event.location.street_no} {event.location.street_name}, {event.location.suburb}, {event.location.postcode}, {event.location.state}, {event.location.country}
                          </Typography>
                        </Grid>
                      </Grid>
                    </Grid>
                    <Grid item xs={12}>
                      <br/>
                      <Typography
                        sx={{
                          color: "#999999"
                        }}
                      >
                        About this event
                      </Typography>
                      <Divider/>
                      <Typography sx={{pt: 2}}>
                        {event.description}
                      </Typography>
                    </Grid>
                    <Grid item xs={12}>
                      <br/>
                      <Box>
                        <Typography
                          sx={{
                            color: "#999999"
                          }}
                        >
                          Host and event Admins
                        </Typography>
                        <Divider sx={{width: "170px"}}/>
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
                      {(event.tags.length > 0)
                        ? <Box>
                            <Typography
                              sx={{
                                fontWeight: 'bold'
                              }}
                            >
                              Tags
                            </Typography>
                            <TagsBar tags={event.tags} editable={false}/>
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
                              {!soldOut
                                ? <TkrButton sx={{fontSize: '19px', width: '100%'}} onClick={() => navigate(`/purchase_ticket/${params.event_id}`)}>
                                    Purchase tickets
                                  </TkrButton>
                                : <TkrButton  disabled sx={{fontSize: '19px', width: '100%', backgroundColor: '#EEEEEE'}}>
                                    Sold Out
                                  </TkrButton>
                              }
                            </CentredBox>
                        }
                      </Box>
                  }
                  <br/>
                </Grid>
              </Grid>
            </EventForm>
            {eventOver
              ? <EventReview isAttendee={isAttendee} event_id={params.event_id} />
              : <>
                </>
            }
            
          </div>
        }
      </Box>
    </BackdropNoBG>
  );
}
