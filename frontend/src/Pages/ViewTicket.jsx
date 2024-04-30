import { Grid, IconButton, Divider, Tooltip, Skeleton, Typography } from "@mui/material"
import { Box } from "@mui/system"
import React from "react"
import { useNavigate, useParams } from "react-router-dom"
import Header from "../Components/Header"
import { BackdropNoBG, CentredBox, UploadPhoto} from "../Styles/HelperStyles"
import ArrowBackIcon from "@mui/icons-material/ArrowBack"
import { getEventData, getTicketDetails } from "../Helpers"
import dayjs from 'dayjs'

export default function ViewTicket({}) {
  const params = useParams()
  const navigate = useNavigate()

  const [ticketDetails, setTicketDetails] = React.useState(null)
  const [event, setEvent] = React.useState(null)

  // Fetch initial ticket data
  React.useEffect(() => {
    getTicketDetails(params.ticket_id, setTicketDetails)
  }, [])

  React.useEffect(() => {
    if (ticketDetails !== null) {
      getEventData(ticketDetails.event_id, setEvent)
    }
  }, [ticketDetails])

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
        <Grid container spacing={2}>
          <Grid item xs={1}>
            <Box sx={{p: 3, pl:10}}>
              <Tooltip title="To event page">
                <IconButton 
                  onClick={()=>{
                    if (ticketDetails === null) {
                      navigate('/')
                    } else {
                      navigate(`/view_event/${ticketDetails.event_id}`)
                    }
                  }}
                >
                  <ArrowBackIcon/>
                </IconButton>
              </Tooltip>
            </Box>
          </Grid>
          <Grid item xs={10}>
            <CentredBox sx={{display: 'flex', flexDirection: 'column', height: '100%', alignItems: 'center'}}>
              <br/>
              <Box sx={{display: 'flex', flexDirection: 'column', width: '60%'}}>
                <Box sx={{boxShadow: 5, backgroundColor: '#FFFFFFF', m: 1, p: 3, borderRadius: 1}}>
                  {(ticketDetails === null || event === null) 
                    ? <>
                        <CentredBox sx={{flexDirection: 'column', width: '100%'}}>
                          <Skeleton variant="rounded" width={560} height={400}/>
                          <Skeleton variant="text"  width={360} sx={{ fontSize: 50 }} />
                          <Skeleton variant="text"  width={260} sx={{ fontSize: 20 }} />
                        </CentredBox>
                        <br/>
                        <Box sx={{backgroundColor: '#EEEEEE', display: 'flex', borderRadius: 2, p: 2}}>
                          <CentredBox sx={{width: '100%', flexDirection: 'column'}}>
                            <Skeleton variant="text" width={360} sx={{ fontSize: 30 }} />
                            <CentredBox sx={{gap: 1}}>
                              <Skeleton variant="text" width={160} sx={{ fontSize: 20 }} />
                              <Divider orientation="vertical" flexItem />
                              <Skeleton variant="text" width={160} sx={{ fontSize: 20 }} />
                            </CentredBox>
                          </CentredBox>
                        </Box>
                      </>
                    : <>
                    <CentredBox sx={{flexDirection: 'column', width: '100%'}}>
                      <UploadPhoto sx={{height: '100%', width: '100%'}} src={event.picture}/>
                      <Typography sx={{fontWeight: 'bold', fontSize: 40, pt: 1, texAlign: 'center'}}>
                        {event.event_name}
                      </Typography>
                      <Typography sx={{fontSize: 20, fontWeight: "regular", color: "#AE759F", texAlign: 'center'}}>
                        {dayjs(event.start_date).format('lll')} - {dayjs(event.end_date).format('lll')}
                      </Typography>
                    </CentredBox>
                    <br/>
                    <Box sx={{backgroundColor: '#EEEEEE', display: 'flex', borderRadius: 2, p: 2}}>
                      <Box sx={{width: '100%'}}>
                        {(event.has_seats)
                          ? <Typography
                              sx={{
                                fontSize: 30,
                                fontWeight: 'bold',
                                textAlign: 'center'
                              }}
                            >
                              {ticketDetails.sectionName}{ticketDetails.seat_num}
                            </Typography>
                          : <Typography
                              sx={{
                                fontSize: 30,
                                fontWeight: 'bold',
                                textAlign: 'center'
                              }}
                            >
                              {ticketDetails.sectionName} x 1
                            </Typography>
                        }
                        <Grid container>
                          <Grid item xs={1} sx={{height: '100%'}}></Grid>
                          <Grid item xs={10}>
                            <CentredBox sx={{gap: 1, height: '100%'}}>
                              <Typography
                                sx={{
                                  fontSize: 20,
                                  textAlign: 'center'
                                }}
                              >
                                {ticketDetails.first_name} {ticketDetails.last_name}
                              </Typography>
                              <Divider orientation="vertical" flexItem />
                              <Typography
                                sx={{
                                  fontSize: 20,
                                  textAlign: 'center'
                                }}
                              >
                                {ticketDetails.email}
                              </Typography>
                            </CentredBox>
                          </Grid>
                          <Grid item xs={1}>
                          </Grid>
                        </Grid>
                      </Box>
                    </Box>
                  </>
                  }
                </Box>
              </Box>
            </CentredBox>
          </Grid>
        </Grid>
        
      </Box>
    </BackdropNoBG>
  )
}