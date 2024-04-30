import { Box } from '@mui/system'
import React from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Header from '../Components/Header'
import { BackdropNoBG, CentredBox, UploadPhoto } from '../Styles/HelperStyles'
import dayjs from 'dayjs'
import { apiFetch, getEventData, getToken } from '../Helpers'
import { Divider, Grid, LinearProgress, Tooltip, IconButton, FormControl, FormHelperText, Collapse, Alert, Typography } from '@mui/material'
import { EventForm } from './CreateEvent'
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import GroupMemberTicket from '../Components/GroupMemberTicket'
import AddGroupMemberTicket from '../Components/AddGroupMemberTicket'

export default function ManageGroup({}) {
  const params = useParams()
  const navigate = useNavigate()
  const groupId = params.group_id
  var calendar = require('dayjs/plugin/calendar')
  dayjs.extend(calendar)

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

  const [groupDetails, setGroupDetails] = React.useState({
    group_members: [],
    pending_invites: [],
    available_reserves: [],
    host_id: '',
    event_id: '',
  })

  const [groupMembers, setGroupMembers] = React.useState([])
  const [pendingInvites, setPendingInvites] = React.useState([])
  const [availableReserves, setAvailableReserves] = React.useState([])

  const getGroupDetails = async () => {
    try{
      const body = {
        auth_token: getToken(),
        group_id: groupId
      }
      const searchParams = new URLSearchParams(body)
      const response = await apiFetch('GET', `/api/group/details?${searchParams}`, null)
      setGroupDetails(response)
      getEventData(response.event_id, setEvent)
      // setGroupMembers(response.group_members)
      // setPendingInvites(response.pending_invites)
      // setAvailableReserves(response.available_reserves)
    } catch (e) {
      console.log(e)
    }
  }

  React.useEffect(() => {
    getGroupDetails()
  }, [])



  // Refetch details on ticket list changes
  React.useEffect(() => {
    getGroupDetails()
  }, [groupMembers, pendingInvites, availableReserves])


  return (
    <BackdropNoBG>
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
        {(groupDetails.event_id === '')
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
                        <Tooltip title="Back to tickets">
                          <IconButton onClick={()=>{
                            navigate(`/view_tickets/${groupDetails.event_id}`) 
                          }}>
                            <ArrowBackIcon/>
                          </IconButton>
                        </Tooltip>
                      </CentredBox>
                    </Grid>
                    <Grid item xs={10}>
                      <CentredBox sx={{flexDirection: 'column'}}>
                        <Typography sx={{fontWeight: 'bold', fontSize: 40, pt: 1}}>
                          Manage Group
                        </Typography>
                      </CentredBox>
                    </Grid>
                    <Grid item xs={1}>
                    </Grid>
                  </Grid>
                  <Divider/>
                  <br/> 
                  <Box sx={{pl: 5, pr: 5}}>
                    {(groupDetails.group_members.length > 0)
                      ? <Box sx={{width: '100%'}}>
                          <Box sx={{display: 'flex', justifyContent: 'space-between'}}>
                            <Typography sx={{fontSize: 30, fontWeight: 'bold', color: '#333333'}}>
                              Group members
                            </Typography>
                          </Box>
                          <CentredBox sx={{flexDirection: 'column'}}>
                            {groupDetails.group_members.map((member, key) => {
                              return (
                                <GroupMemberTicket key={key} ticket={member} groupId={groupId} setTickets={setGroupMembers} tickets={groupMembers} index={key}/>
                              )
                            })}
                          </CentredBox>
                        </Box>
                      : <></>
                    }
                    {(groupDetails.pending_invites.length > 0)
                      ? <Box sx={{width: '100%'}}>
                          <Box sx={{display: 'flex', justifyContent: 'space-between'}}>
                            <Typography sx={{fontSize: 30, fontWeight: 'bold', color: '#333333'}}>
                              Pending invites
                            </Typography>
                          </Box>
                          <CentredBox sx={{flexDirection: 'column'}}>
                            {groupDetails.pending_invites.map((member, key) => {
                              return (
                                <GroupMemberTicket key={key} ticket={member} groupId={groupId} setTickets={setPendingInvites} tickets={pendingInvites} index={key}/>
                              )
                            })}
                          </CentredBox>
                        </Box>
                      : <></>
                    }
                    {(groupDetails.available_reserves.length > 0)
                      ? <Box sx={{width: '100%'}}>
                          <Box sx={{display: 'flex', justifyContent: 'space-between'}}>
                            <Typography sx={{fontSize: 30, fontWeight: 'bold', color: '#333333'}}>
                              Availalble reserves
                            </Typography>
                          </Box>
                          <CentredBox sx={{flexDirection: 'column'}}>
                            {groupDetails.available_reserves.map((member, key) => {
                              return (
                                <AddGroupMemberTicket key={key} ticket={member} groupId={groupId} setTickets={setAvailableReserves} tickets={availableReserves} index={key}/>
                              )
                            })}
                          </CentredBox>
                        </Box>
                      : <></>
                    }
                  </Box>
                  
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
                  </Box>
                </Grid>
              </Grid>
            </EventForm>
        }

      </Box>
    </BackdropNoBG>
  )
}