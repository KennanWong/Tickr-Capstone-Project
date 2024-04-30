import { Box } from "@mui/system"
import React from "react"
import Header from "../Components/Header"
import { BackdropNoBG, CentredBox, H3, UploadPhoto } from "../Styles/HelperStyles"
import { useNavigate, useParams } from "react-router-dom";
import dayjs from "dayjs";
import { borderRadius, styled, alpha } from '@mui/system';
import { checkValidEmail, getEventData, getToken, setFieldInState } from "../Helpers";
import { Alert, Divider, FormControl, FormControlLabel, FormGroup, FormHelperText, FormLabel, Grid, IconButton, InputLabel, LinearProgress, MenuItem, Select, Tooltip, Typography } from "@mui/material";
import { EventForm } from "./ViewEvent";
import ShoppingCartOutlinedIcon from '@mui/icons-material/ShoppingCartOutlined';
import SeatSelector from "../Components/SeatSelector";
import QuantitySelector from "../Components/QuantitySelector";
import { ContrastInput, ContrastInputWrapper, TkrButton } from "../Styles/InputStyles";
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import ShadowInput from "../Components/ShadowInput";
import Collapse from '@mui/material/Collapse';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import Switch from '@mui/material/Switch';

const ExpandMore = styled((props) => {
  const { expand, ...other } = props;
  return <IconButton {...other} />;
})(({ theme, expand }) => ({
  transform: !expand ? 'rotate(0deg)' : 'rotate(180deg)',
  marginLeft: 'auto',
  transition: theme.transitions.create('transform', {
    duration: theme.transitions.duration.shortest,
  }),
}));

function Section ({section, getTicketDetails, handleTicketInput, handleSectionExpanded}) {
  return (
    <Box sx={{display: 'flex', justifyContent: 'center', boxShadow: 5, p: 1, borderRadius: 2, flexDirection: 'column'}}>
      <Grid container spacing={2}>
        <Grid item xs={8}>
          <Typography sx={{fontSize: 30}}>
            {section.quantity} x {section.section}
          </Typography>
        </Grid>
        <Grid item xs={4}>
          <Box sx={{display:'flex', width: '100%', justifyContent: 'flex-end'}}>
            <ExpandMore
              sx={{
                m:0
              }}
              expand={section.expanded}
              onClick={(e)=> {handleSectionExpanded(section)}}
              aria-expanded={section.expanded}
              aria-label="show more"
            >
              <ExpandMoreIcon />
            </ExpandMore>
          </Box>
          
        </Grid>
      </Grid>
      <Collapse in={section.expanded}>
        {section.seat_numbers.map((seat_num, key) => {
          return (
            <Ticket key={key} seatNum={seat_num} section={section} getTicketDetails={getTicketDetails} handleTicketInput={handleTicketInput}/>
          )
        })}
      </Collapse>
    </Box>
  )
}

function Ticket ({seatNum, section, getTicketDetails, handleTicketInput}) {
  return (
    <Box sx={{pt: 1, pb: 1}}>
      <Grid container spacing={2}>
        <Grid item xs={2}>
        </Grid>
        <Grid item xs={2}>
          <CentredBox sx={{backgroundColor: alpha('#6A7B8A', 0.3), height: '100%', width: '100%', borderRadius: 3}}>
            <Typography xs={{}}>
              {section.section[0]}{seatNum}
            </Typography>
          </CentredBox>
        </Grid>
        <Grid item xs>
          <Grid container spacing={1}>
            <Grid item xs={6}>
              <ContrastInputWrapper>
                <ContrastInput
                  fullWidth
                  placeholder="First Name"
                  onChange={(e) => {handleTicketInput(seatNum, 'first_name', e.target.value)}}
                  value={getTicketDetails('first_name', seatNum)}
                >
                </ContrastInput>
              </ContrastInputWrapper>
            </Grid>
            <Grid item xs={6}>
              <ContrastInputWrapper>
                <ContrastInput
                  fullWidth
                  placeholder="Last Name"
                  onChange={(e) => {handleTicketInput(seatNum, 'last_name', e.target.value)}}
                  value={getTicketDetails('last_name', seatNum)}
                >
                </ContrastInput>
              </ContrastInputWrapper>
            </Grid>
            <Grid item xs={12}>
              <ContrastInputWrapper>
                <ContrastInput
                  placeholder="Email"
                  fullWidth
                  onChange={(e) => {handleTicketInput(seatNum, 'email', e.target.value)}}
                  value={getTicketDetails('email', seatNum)}
                >
                </ContrastInput>
              </ContrastInputWrapper>
            </Grid>
          </Grid>
        </Grid>
        <Grid item xs={2}>
        </Grid>
      </Grid>
    </Box>
  )
}

export default function Checkout ({ticketOrder}) {
  const params = useParams()
  const navigate = useNavigate()
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
    host_id: ''
  })

  const [sectionDetails, setSectionDetails] = React.useState([])

  const [orderDetails, setOrderDetails] = React.useState(ticketOrder.ticket_details)

  const [selectedSeats, setSelectedSeats] = React.useState([])
  const [orderTotal, setOrderTotal] = React.useState(0)

  const [error, setError] = React.useState(false)
  const [errorMsg, setErrorMsg] = React.useState('')

  const [customNames, setCustomNames] = React.useState(false)

  const [ticketDetails, setTicketDetails] = React.useState([])

  const [userDetails, setUserDetails] = React.useState({
    firstName: '',
    lastName: '',
    email: ''
  })


  React.useEffect(()=> {
    try {
      getEventData(params.event_id, setEvent)
      const ticketDetails_t = []
      const orderDetails_t = []
      ticketOrder.ticket_details.forEach(function (section) {
        section['expanded'] = false
        
        // Fill in section details
        if (section.seat_number.length !== 0) {
          section.seat_numbers.forEach(function(seat_num) {
            const body = {
              first_name: '',
              last_name: '',
              email: '',
              seat_num: seat_num,
              section: section.section,
              reserve_id: ticketOrder.reserve_Tickets.forEach(function (reserve) {
                if (reserve.seat_number === seat_num) {
                  return reserve.reserve_id
                }
              })
            }
            ticketDetails_t.push(body)
          })
          orderDetails_t.push(section)
        } else {
          ticketOrder.reserve_Tickets.forEach(function (reserve) {
            if (reserve.section === section.section) {
              reserve['first_name'] = ''
              reserve['last_name'] = ''
              reserve['email'] = ''
              ticketDetails_t.push(reserve)
              section.seat_number.push(reserve.seat_number)
            }
          })
          orderDetails_t.push(section)
        }
      })
      setTicketDetails(ticketDetails_t)
      setOrderDetails(orderDetails_t)

    } catch (e) {
      // navigate(`/purchase_ticket/${params.event_id}`)
    }
    
  },[ticketOrder])

  React.useEffect(() => {
    var total = 0
    for (const m in orderDetails) {
      const order = orderDetails[m]
      total += order.ticket_price*order.quantity
    }
    setOrderTotal(total)
    if (error) {
      setError(false)
      setErrorMsg('')
    }
  },[sectionDetails])

  const allSeatsSelected = () => {
    var allSelected = true
    for (const i in sectionDetails) {
      if ((sectionDetails[i].quantity !== sectionDetails[i].seatsSelected.length) && sectionDetails[i].selectable) {
        allSelected = false
      }
    }
    return allSelected
  }

  const handlePayment = async () => {

    var errorStatus = false
    // If custom names, check all fields are filled
    if (customNames) {
      ticketDetails.forEach(function (ticket) {
        if (ticket.first_name === '' || ticket.last_name === '' || ticket.email === '' || !checkValidEmail(ticket.email)) {
          errorStatus = true
        }
      })
      if (errorStatus) {
        setError(true)
        setErrorMsg("Invalid form details. Check all fields have been filled.")
        return
      }
    } else {
      if (userDetails.firstName === '' || userDetails.lastName === '' || userDetails.email === '' || !checkValidEmail(userDetails.email)) {
        setError(true)
        setErrorMsg("Invalid form details. Check all fields have been filled.")
        return
      } 
      // Fill in remaining ticket details
      const newState = ticketDetails.map(ticket => {
        return {...ticket, first_name: userDetails.firstName, last_name: userDetails.lastName, email: userDetails.email}
      })
      setTicketDetails(newState)
    }
  }


  const handleTicketInput = (seat_num, field, value) => {
    // Find tciket within ticket details
    setError(false)
    setErrorMsg('')
    const newState = ticketDetails.map(ticket => {
      if (ticket.seat_num ===  seat_num) {
        ticket[field] = value
        return ticket;
      }
      return ticket
    })
    setTicketDetails(newState)
  }

  const handleSectionExpanded = (section) => {
    const newState = orderDetails.map(obj => {
      if (obj === section) {
        return {...obj, expanded: !section.expanded}
      }
      return obj
    })
    setOrderDetails(newState)
  }

  const getTicketDetails = (field, seatNum) => {
    for (const i in ticketDetails) {
      const ticket = ticketDetails[i]
      if (ticket.seat_num === seatNum) {
        return ticket[field]
      }
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
                      {/* can be reintroduced later */}
                      {/* <Tooltip title="Back to ticket menu">
                        <IconButton onClick={()=>{navigate(`/purchase_ticket/${params.event_id}`)}}>
                          <ArrowBackIcon/>
                        </IconButton>
                      </Tooltip> */}
                    </CentredBox>
                    </Grid>
                    <Grid item xs={10}>
                      <CentredBox sx={{flexDirection: 'column'}}>
                        <Typography sx={{fontWeight: 'bold', fontSize: 40, pt: 1}}>
                          Order Checkout
                        </Typography>
                      </CentredBox>
                    </Grid>
                    <Grid item xs={1}>
                    </Grid>
                  </Grid>
                  <Divider/>
                  <br/>
                  <CentredBox sx={{flexDirection: 'column', ml: 5, mr: 5}}>
                    <Grid container spacing={2}>
                      <Grid item xs={3}/>
                      <Grid item xs={6}>
                        <Typography sx={{fontWeight: 'bold', fontSize: 30, pt: 1}}>
                          Provide Ticket Details
                        </Typography>
                      </Grid>
                      <Grid item xs={3}>
                        <FormGroup >
                          <FormLabel component="legend">Assign details per ticket</FormLabel>
                          <FormControlLabel sx={{display: 'flex', justifyContent: 'flex-end'}} control={<Switch onChange={(e) => {setCustomNames(e.target.checked)}}/>}/>
                        </FormGroup>
                      </Grid>
                    </Grid>
                    {customNames
                      ? <Box sx={{p: 2, borderRadius: 2, width: '100%'}}>
                          {orderDetails.map((section, key) => {
                            return (
                              <Section key={key} section={section} getTicketDetails={getTicketDetails} handleTicketInput={handleTicketInput} handleSectionExpanded={handleSectionExpanded}/>
                            )
                          })}
                        </Box>
                      : <Grid container spacing={2}>
                          <Grid item xs={2}></Grid>
                          <Grid item xs={4}>
                            <ContrastInputWrapper>
                              <ContrastInput
                                fullWidth
                                placeholder="First Name"
                                value={userDetails.firstName}
                                onChange={(e) => {
                                  setError(false)
                                  setErrorMsg('')
                                  setFieldInState('firstName', e.target.value, userDetails, setUserDetails)
                                }}
                              >
                              </ContrastInput>
                            </ContrastInputWrapper>
                          </Grid>
                          <Grid item xs={4}>
                            <ContrastInputWrapper>
                              <ContrastInput
                                fullWidth
                                placeholder="Last Name"
                                value={userDetails.lastName}
                                onChange={(e) => {
                                  setError(false)
                                  setErrorMsg('')
                                  setFieldInState('lastName', e.target.value, userDetails, setUserDetails)
                                }}
                              >
                              </ContrastInput>
                            </ContrastInputWrapper>
                          </Grid>
                          <Grid item xs={2}></Grid>
                          <Grid item xs={2}></Grid>
                          <Grid item xs={8}>
                            <ContrastInputWrapper>
                              <ContrastInput
                                placeholder="Email"
                                fullWidth
                                value={userDetails.email}
                                onChange={(e) => {
                                  setError(false)
                                  setErrorMsg('')
                                  setFieldInState('email', e.target.value, userDetails, setUserDetails)
                                }}
                              >
                              </ContrastInput>
                            </ContrastInputWrapper>
                          </Grid>
                          <Grid item xs={2}></Grid>
                        </Grid>

                    }
                  </CentredBox>
                </Grid>
                <Divider orientation="vertical" flexItem></Divider>
                <Grid item xs>
                  <Box sx={{boxShadow: 5, backgroundColor: '#FFFFFFF', m: 1, p: 3, borderRadius: 1}}>
                    <CentredBox sx={{flexDirection: 'column', widht: '100%'}}>
                      <UploadPhoto src={event.picture}/>
                      <Typography sx={{fontWeight: 'bold', fontSize: 40, pt: 1, texAlign: 'center'}}>
                        {event.event_name}
                      </Typography>
                      <Typography sx={{fontSize: 15, fontWeight: "regular", color: "#AE759F", texAlign: 'center'}}>
                        {dayjs(event.start_date).format('lll')} - {dayjs(event.end_date).format('lll')}
                      </Typography>
                    </CentredBox>
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
                        <Grid container spacing={2} sx={{pl: 1, pr: 1}}>
                          {orderDetails.map((section, key) => {
                            return (
                              <Grid item key={key} sx={{width: '100%'}}>
                                <Grid container spacing={2} sx={{pl: 1, pr: 1}}>
                                  <Grid item xs={9}>
                                    <Typography sx={{fontSize: 20}}>
                                      {section.quantity} x {section.section} - ${section.ticket_price}
                                    </Typography>
                                    {(section.seat_number)
                                      ? <Box sx={{display: 'flex', gap: 1, flexWrap: 'wrap'}}>
                                          {section.seat_number.map((seat, key) => {
                                            if (key !== section.seat_number.length - 1) {
                                              return (
                                                <Typography key={key} sx={{color: 'rgba(0, 0, 0, 0.6)'}}>
                                                  {section.section[0]}{seat},
                                                </Typography>
                                              )
                                            } else {
                                              return (
                                                <Typography key={key} sx={{color: 'rgba(0, 0, 0, 0.6)'}}>
                                                  {section.section[0]}{seat}
                                                </Typography>
                                              )
                                            }
                                          })}
                                          
                                        </Box>
                                      : <>
                                        </>
                                    }
                                  </Grid>
                                  <Grid item xs={3}>
                                    <Typography sx={{fontSize: 20, textAlign: 'right'}}>
                                      ${(section.ticket_price*section.quantity)}
                                    </Typography>
                                  </Grid>
                                </Grid>
                                <br/>
                              </Grid>
                            )
                          })}
                        </Grid>
                        <br/>
                        <Divider/>
                        <br/>
                        <Grid container spacing={2} sx={{pl: 2, pr: 2}}>
                          <Grid item xs={9}>
                            <Typography sx={{fontSize: 25, fontWeight: 'bold'}}>
                              Total: 
                            </Typography>
                          </Grid>
                          <Grid item xs={3}>
                            <Typography sx={{fontSize: 25, textAlign: 'right'}}>
                              ${orderTotal} 
                            </Typography>
                          </Grid>
                        </Grid>
                      </Box>
                    </Box>
                    <br/>
                    <TkrButton
                      sx={{width: '100%'}}
                      onClick={handlePayment}
                      startIcon={<ShoppingCartOutlinedIcon/>}
                    >
                      Checkout
                    </TkrButton>
                    <br/>
                    <br/>
                    <Collapse in={error}>
                      <CentredBox>
                        <Alert severity="error">{errorMsg}</Alert>
                      </CentredBox>
                    </Collapse>
                  </Box>
                </Grid>
              </Grid>

            </EventForm>
        }
      </Box>
    </BackdropNoBG>
  )
}