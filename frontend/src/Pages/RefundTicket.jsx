import { Divider, Grid, IconButton, LinearProgress, Tooltip, Typography } from '@mui/material'
import { Box } from '@mui/system'
import React from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Header from '../Components/Header'
import TicketCard from '../Components/TicketCard'
import { apiFetch, getEventData, getToken } from '../Helpers'
import { BackdropNoBG, CentredBox } from '../Styles/HelperStyles'
import { EventForm } from './CreateEvent'
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { DeleteButton } from '../Styles/InputStyles'

export default function RefundTicket({}) {
  const params = useParams()
  const navigate = useNavigate()

  const [ticketDetails, setTicketDetails] = React.useState(null)
  const [eventDetails, setEventDetails] = React.useState(null)

  // Get Ticket Data
  React.useEffect(()=> {
    getTicketData() 
  },[])

  const getTicketData = async () => {
    // Send API call to get ticket details
    const paramsObj = {
      ticket_id: params.ticket_id,
    }
    const searchParams = new URLSearchParams(paramsObj)
    try {
      const response = await apiFetch('GET', `/api/ticket/view?${searchParams}`)
      setTicketDetails(response)
      getEventData(response.event_id, setEventDetails)
    } catch (e) {
      console.log(e)
    } 
  }

  const handleRefund = async () => {
    try {
      const body = {
        auth_token: getToken(),
        ticket_id: params.ticket_id
      }

      const response = await apiFetch('POST', '/api/ticket/refund', body)
      navigate("/")
    } catch (e) {
      console.log(e)
    }
  }

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
        {eventDetails === null
          ? <Box sx={{height: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center', margin: '50px', paddingTop: '100px'}}> 
              <Box sx={{ width: '90%', hieght: 50}}>
                <LinearProgress color='secondary'/>
              </Box>
            </Box>
          :  <EventForm>
              <Grid container spacing={2}>
                <Grid item xs={5}>
                  <Grid container>
                    <Grid item xs={1}>
                      <CentredBox sx={{height: '100%'}}>
                        <Tooltip title="Back to tickets">
                          <IconButton onClick={() => {navigate(`/view_tickets/${ticketDetails.event_id}`)}}>
                            <ArrowBackIcon/>
                          </IconButton>
                        </Tooltip>
                      </CentredBox>
                    </Grid>
                    <Grid item xs={10}>
                      <CentredBox sx={{flexDirection: 'column'}}>
                        <Typography sx={{fontWeight: 'bold', fontSize: 40, pt: 1}}>
                          Refund Ticket
                        </Typography>
                      </CentredBox>
                    </Grid>
                    <Grid item xs={1}>
                    </Grid>
                  </Grid>
                  <Divider/>
                  <br/> 
                  <CentredBox sx={{flexDirection: 'column', pl: 3, pr:3, pt: 10}}>
                    <Typography sx={{fontFamily: 'Segoe UI', fontSize: 30, textAlign: 'center'}}>
                      Are you sure you want to refund your ticket to {eventDetails.event_name}?
                    </Typography>
                    <br/>
                    <DeleteButton onClick={handleRefund}>Refund</DeleteButton>
                  </CentredBox>
                </Grid>
                <Divider flexItem orientation='vertical'/>
                <Grid item xs>
                  <CentredBox sx={{width: 'center'}}>
                    <TicketCard event={eventDetails} ticket_id={params.ticket_id} ticketOwner={false}/> 
                  </CentredBox>
                </Grid>
              </Grid>
            </EventForm>
        }
      </Box>
    </BackdropNoBG>
  )
}