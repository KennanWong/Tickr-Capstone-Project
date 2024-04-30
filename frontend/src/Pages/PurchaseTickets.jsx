import { Box } from "@mui/system"
import React from "react"
import Header from "../Components/Header"
import { BackdropNoBG, CentredBox, H3, UploadPhoto } from "../Styles/HelperStyles"
import { useNavigate, useParams } from "react-router-dom";
import dayjs from "dayjs";
import { apiFetch, checkValidEmail, getEventData, getToken, setFieldInState, setReservedTicketsLocal, sortSection } from "../Helpers";
import { Alert, Collapse, Divider, FormControl, FormControlLabel, FormGroup, FormHelperText, FormLabel, Grid, IconButton, InputLabel, LinearProgress, MenuItem, Select, Tooltip, Typography } from "@mui/material";
import { EventForm } from "./ViewEvent";
import ShoppingCartOutlinedIcon from '@mui/icons-material/ShoppingCartOutlined';
import SeatSelector from "../Components/SeatSelector";
import QuantitySelector from "../Components/QuantitySelector";
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { borderRadius, styled, alpha } from '@mui/system';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import Switch from '@mui/material/Switch';
import { ContrastInput, ContrastInputWrapper, TextButton2, TkrButton } from "../Styles/InputStyles";
import SectionDetails from "../Components/SectionDetails";
import GroupsIcon from '@mui/icons-material/Groups';
import GroupTickets from "../Components/GroupTickets";
import OrderDetails from "../Components/OrderDetails";


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


export default function PurchaseTicket ({setTicketOrder, ticketOrder}) {
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

  const [selectedSeats, setSelectedSeats] = React.useState([])
  const [orderTotal, setOrderTotal] = React.useState(0)

  const [error, setError] = React.useState(false)
  const [errorMsg, setErrorMsg] = React.useState('')

  const [error2, setError2] = React.useState(false)
  const [errorMsg2, setErrorMsg2] = React.useState('')

  // States for collapsable sections
  const [ticketSelect, setTicketSelect] = React.useState(true)
  const [detailsInput, setDetailsInput] = React.useState(false)

  const [customNames, setCustomNames] = React.useState(false)
  const [areTicketsReserved, setAreTicketsReserved] = React.useState(false)

  // Reserved tickets
  const [reservedTickets, setReservedTickets] = React.useState([])

  // Selected tickets
  const [orderDetails, setOrderDetails] = React.useState([])

  const [userDetails, setUserDetails] = React.useState({
    firstName: '',
    lastName: '',
    email: ''
  })

  const [groupTicketBuy, setGroupTicketBuy] = React.useState(false)
  const [groupLeaderTicket, setGroupLeaderTicket] = React.useState({
    section: '',
    seat_number: '',
    price: ''
  })
  const [groupCart, setGroupCart] = React.useState(0)

  // If group ticket purchase, update cart to reflect this
  React.useEffect(() => {
    var selectable_t = false
    var sectionPrice = 0
    selectedSeats.forEach(function(section) {
      if (section.section === groupLeaderTicket.section) {
        selectable_t = section.selectable
      }
    })
    setGroupLeaderTicket({...groupLeaderTicket, selectable: selectable_t})
    // setGroupCart(groupLeaderTicket.price)
    if (!groupTicketBuy) {
      const init = {
        section: '',
        seat_number: '',
        price: ''
      }
      setGroupLeaderTicket(init)

    }
  }, [groupTicketBuy])

  // Get section details and available seats
  const getSectionDetails = async () => {
    const sectionDetails_t = []
    for (const m in event.seating_details) {
      const section = event.seating_details[m]
      var seats = []
      var i = 0
      while (i <= 10 && i <= section.available_seats) {
        seats.push(i)
        i++
      }
      const section_det = {
        ticket_price: section.ticket_price,
        section: section.section,
        available_seats: section.available_seats,
        capacity: section.total_seats,
        seats: seats,
        quantity: 0,
        selectable: section.has_seats,
        seatsSelected: [],
        takenSeats: []
      }
      sectionDetails_t.push(section_det)
    }
    // sortSection(sectionDetails_t)
    
    // Check booked seats
    try {
      const body = {
        auth_token: getToken(),
        event_id: params.event_id
      }
      const param = new URLSearchParams(body)
      const response = await apiFetch('GET', `/api/event/attendees?${param}`, null)
      const attendees = response.attendees
      // Sort through tickets bought and attach to respective section
      attendees.forEach(function(attendee) {
        const tickets = attendee.tickets
        tickets.forEach(async function (ticket) {
          const ticketData = await apiFetch('GET', `/api/ticket/view?ticket_id=${ticket}`)
          sectionDetails_t.forEach(function (section) {
            if (section.section === ticketData.section && section.selectable) {
              section.takenSeats.push(section.section[0]+ticketData.seat_num)
            }
          })
        })
      })
    } catch (e) {
      console.log(e)
    }

    // Check reserved seats
    try {
      const body2 = {
        auth_token: getToken(),
        event_id: params.event_id
      }
      const param2 = new URLSearchParams(body2)
      const response2 = await apiFetch('GET', `/api/event/reserved?${param2}`, null)
      const reserved = response2.reserved

      // Sort through tickets bought and attach to respective section
      sectionDetails_t.forEach(function (section) {
        reserved.forEach(function(reserve) {
          if (section.section === reserve.section && section.selectable) {
            section.takenSeats.push(section.section[0]+reserve.seat_number)
          }
        })
      })
    } catch (e) {
      console.log(e)
    }

    setSectionDetails(sectionDetails_t)
  } 

  // Set up seat selector
  React.useEffect(() => {
    if (!areTicketsReserved) {
      getSectionDetails()
    }
  }, [event.event_name])



  React.useEffect(() => {
    setSelectedSeats([])
    var total = 0
    const new_selected = []
    for  (const i in sectionDetails) {
      if (sectionDetails[i].quantity > 0) {
        const section = sectionDetails[i]
        new_selected.push(section)
        total += (section.ticket_price*section.quantity)
      }
    }
    setSelectedSeats(new_selected)
    setOrderTotal(total)
  }, [sectionDetails])

  // Initial get event data
  React.useEffect(()=> {
    getEventData(params.event_id, setEvent)
  },[])

  React.useEffect(() => {
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


  const handleTicketInput = (reserve_id, field, value) => {
    // Find ticket within ticket details
    setError(false)
    setErrorMsg('')
    const newState = reservedTickets.map(ticket => {
      if (ticket.reserve_id ===  reserve_id) {
        ticket[field] = value
        return ticket;
      }
      return ticket
    })
    setReservedTickets(newState)
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

  const getTicketDetails = (field, reserve_id) => {
    for (const i in reservedTickets) {
      const ticket = reservedTickets[i]
      if (ticket.reserve_id === reserve_id) {
        return ticket[field]
      }
    }
  }

  // User chooses to edit their selection
  const handleEditSelection = async () => {
    // set tickets reserved to false, cancel reserves
    try {
      // cancel all reservations
      const reserveIds = []
      reservedTickets.forEach(function (reserve) {
        reserveIds.push(reserve.reserve_id)
      })

      const body = {
        auth_token: getToken(),
        reservations: reserveIds
      }

      const response = await apiFetch('DELETE', '/api/ticket/reserve/cancel', body)
    } catch (e) {
      console.log(e)
      return
    }
    setAreTicketsReserved(false)
    setTicketSelect(true)
    setDetailsInput(false)
    return
  }

  const handleReserve = async () => {
    // Prepare data package
    const ticketDetails = []
    sectionDetails.forEach(function (section) {
      if (section.quantity > 0) {
        const seatsSelected = []

        section.seatsSelected.forEach(function (seat) {
          seatsSelected.push(seat.slice(1))
        })
        const sectionBody = {
          section: section.section,
          quantity: section.quantity,
          seat_numbers: seatsSelected.length > 0 ? seatsSelected : [],
          ticket_price: section.ticket_price
        }

        ticketDetails.push(sectionBody)
      }
    })

    const body = {
      auth_token: getToken(),
      event_id: params.event_id,
      ticket_datetime: event.start_date,
      ticket_details: ticketDetails,
    }

    try {
      // Send API call
      const response = await apiFetch('POST', '/api/ticket/reserve', body)
      
      const reservedTickets_t = []
      setTicketOrder(response.reserve_tickets)
      setReservedTicketsLocal(response.reserve_tickets)

      response.reserve_tickets.forEach((function (reserve) {
        reserve['first_name'] = ''
        reserve['last_name'] = ''
        reserve['email'] = ''
        reserve['request_id'] = reserve.reserve_id
        reservedTickets_t.push(reserve)
      }))
      setReservedTickets(reservedTickets_t)

      // Set section details with matching reserveid
      const orderDetails_t = []
      ticketDetails.forEach(function (section) {
        section['expanded'] = false
        section['reserved_seats'] = []
        response.reserve_tickets.forEach((function (reserve) {
          if (reserve.section === section.section)
            section['reserved_seats'].push(reserve)
        }))
        orderDetails_t.push(section)
      })
      setOrderDetails(orderDetails_t)

      body['reserve_tickets'] = response.reserve_tickets

      setAreTicketsReserved(true)
      setTicketSelect(false)
      setDetailsInput(true)
    } catch (error) {
      // Display error message in error collapse
      setError(true)
      setErrorMsg(error.reason)
    }
  }

  const openGroupTicketBuy = () => {
    setGroupTicketBuy(true)
  }

  React.useEffect(() => {
    if (error2)  {
      setError2(false)
      setErrorMsg2('')
    }
  }, [reservedTickets])

  // Purchase ticket
  const handlePayment = async () => {
    var errorStatus = false
    // If custom names, check all fields are filled
    if (customNames) {
      reservedTickets.forEach(function (ticket) {
        if (ticket.first_name === '' || ticket.last_name === '' || ticket.email === '' || !checkValidEmail(ticket.email)) {
          errorStatus = true
        }
      })
      if (errorStatus) {
        setError2(true)
        setErrorMsg2("Invalid form details. Check all fields have been filled.")
        return
      }
      const body = {
        auth_token: getToken(),
        ticket_details: reservedTickets,
        success_url: `http://localhost:3000/view_tickets/${params.event_id}`,
        cancel_url: `http://localhost:3000/cancel_reservation`
      }
      try {
        const response = await apiFetch('POST', '/api/ticket/purchase', body)
        window.location.replace(response.redirect_url)

      } catch (error) {
        console.log(error)
      }
    } else {
      if (userDetails.first_name === '' || userDetails.last_name === '' || userDetails.email === '' || !checkValidEmail(userDetails.email)) {
        setError2(true)
        setErrorMsg2("Invalid form details. Check all fields have been filled.")
        return
      } 
      // Fill in remaining ticket details
      const newState = reservedTickets.map(ticket => {
        return {...ticket, first_name: userDetails.firstName, last_name: userDetails.lastName, email: userDetails.email}
      })

      const body = {
        auth_token: getToken(),
        ticket_details: newState,
        success_url: `http://localhost:3000/view_tickets/${params.event_id}`,
        cancel_url: `http://localhost:3000/cancel_reservation`
      }

      try {
        const response = await apiFetch('POST', '/api/ticket/purchase', body)
        window.location.replace(response.redirect_url)

      } catch (error) {
        console.log(error)
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
                        <Tooltip title="Back to event">
                          <IconButton onClick={()=>{
                            if (areTicketsReserved) {
                              window.location.replace(`http://localhost:3000/cancel_reservation/${params.event_id}`)
                            } else {
                              navigate(`/view_event/${params.event_id}`)
                            }
                            
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
                  <br/>
                  <Box sx={{pl: 1, pr: 1}}>
                    {/* Select Tickets */}
                    <Box sx={{display: 'flex', justifyContent: 'center', p: 1, borderRadius: 2, flexDirection: 'column', backgroundColor: ticketSelect ? '#FFFFFF' : '#EEEEEE'}}>
                      <Grid container spacing={2}>
                        <Grid item xs={8}>
                          <Typography sx={{fontSize: 30, fontWeight: 'bold', color: !areTicketsReserved ? 'black' : 'rgba(0, 0, 0, 0.26)'}}>
                            1. Select Tickets
                          </Typography>
                        </Grid>
                        <Grid item xs={4}>
                          <Box sx={{display:'flex', width: '100%', height: '100%', justifyContent: 'flex-end', alignItems: 'center'}}>
                            {areTicketsReserved
                              ? <TextButton2 onClick={handleEditSelection}>
                                  Edit selection
                                </TextButton2>
                              : <></>
                            }
                          </Box>
                        </Grid>
                      </Grid>
                      <Collapse in={ticketSelect}>
                        {(sectionDetails).map((section, key) => {
                          if (section.available_seats > 0) {
                            return (
                              <div key={key}>
                                {section.selectable
                                  ? <SeatSelector section={section} key={key} index={key} sectionDetails={sectionDetails} setSectionDetails={setSectionDetails}/>
                                  : <QuantitySelector section={section} key={key} index={key} sectionDetails={sectionDetails} setSectionDetails={setSectionDetails}/>
                                }
                              </div>
                            )
                          }
                        })}
                        <Divider variant="middle" />
                        <br/>
                        <Box sx={{ml: 5, mr: 5}}>
                          <CentredBox sx={{width: '100%', flexDirection: 'column'}}>
                            {(allSeatsSelected() && selectedSeats.length !== 0)
                              ? <TkrButton
                                sx={{width: '100%'}}
                                  onClick={handleReserve}
                                >
                                  Confirm Tickets
                                </TkrButton>
                              : <FormControl sx={{width: '100%'}}>
                                  <TkrButton
                                    sx={{width: '100%'}}
                                    disabled={true}
                                  >
                                    Checkout
                                  </TkrButton>
                                  <FormHelperText sx={{display:'flex', justifyContent: 'center'}} required={true}>
                                    <Typography sx={{color: 'rgba(0, 0, 0, 0.6)', textAlign: 'center'}} component={'span'}>
                                      Select all seats to purchase
                                    </Typography>
                                  </FormHelperText>
                                </FormControl>
                            }
                            <br/>
                            <Collapse in={error}>
                              <Alert severity="error">{errorMsg}</Alert>
                            </Collapse>
                          </CentredBox>
                        </Box>
                      </Collapse>
                    </Box>
                    <br/>
                    {/* Enter in Order Details */}
                    <Box sx={{display: 'flex', justifyContent: 'center', p: 1, borderRadius: 2, flexDirection: 'column', backgroundColor: detailsInput ? '#FFFFFF' : '#EEEEEE'}}>
                      <Grid container spacing={2}>
                        <Grid item xs={8}>
                          <Typography sx={{fontSize: 30, fontWeight: 'bold', color: areTicketsReserved ? 'black' : 'rgba(0, 0, 0, 0.26)'}}>
                            2. Enter Order Details
                          </Typography>
                        </Grid>
                        <Grid item xs={4}>
                          {/* <Box sx={{display:'flex', width: '100%', justifyContent: 'flex-end'}}>
                            <ExpandMore
                              sx={{
                                m:0
                              }}
                              expand={detailsInput}
                              onClick={(e)=> {setDetailsInput(!detailsInput)}}
                              aria-expanded={detailsInput}
                              aria-label="show more"
                              disabled={!areTicketsReserved}
                            >
                              <ExpandMoreIcon />
                            </ExpandMore>
                          </Box> */}
                        </Grid>
                      </Grid>
                      <Collapse in={detailsInput}>
                        <Collapse in={!groupTicketBuy}>
                          <CentredBox sx={{flexDirection: 'column', ml: 5, mr: 5}}>
                            <br/>
                            {/* Custom Name Seating */}
                            <Grid container spacing={2}>
                              <Grid item xs={6}>
                              </Grid>
                              <Grid item xs={6}>
                                {(reservedTickets.length > 1)
                                  ? <Box sx={{width: '100%', display: 'flex', justifyContent: 'flex-end'}}>
                                      <FormGroup >
                                        <FormControlLabel label="Assign details per ticket" labelPlacement="start" sx={{display: 'flex', justifyContent: 'flex-end'}} control={<Switch onChange={(e) => {setCustomNames(e.target.checked)}}/>}/>
                                      </FormGroup>
                                    </Box>  
                                  : <></>
                                }
                              </Grid>
                            </Grid>
                            {customNames
                              ? <Box sx={{p: 2, borderRadius: 2, width: '100%'}}>
                                  {orderDetails.map((section, key) => {
                                    return (
                                      <SectionDetails key={key} section={section} getTicketDetails={getTicketDetails} handleTicketInput={handleTicketInput} handleSectionExpanded={handleSectionExpanded}/>
                                    )
                                  })}
                                </Box>
                              : <OrderDetails setError={setError2} userDetails={userDetails} setUserDetails={setUserDetails} setErrorMsg={setErrorMsg2} />
                            }
                            <br/>
                            <Grid container>
                              <Grid xs={8} item>
                              </Grid>
                              <Grid xs item>
                                {(reservedTickets.length > 1)
                                  ? <Box sx={{display: 'flex', justifyContent: 'flex-end'}}>
                                      <TextButton2 startIcon={<GroupsIcon/>} onClick={openGroupTicketBuy}>
                                        Buying as a group?
                                      </TextButton2> 
                                    </Box>
                                  : <></>
                                }
                              </Grid>
                            </Grid>
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
                            <Collapse in={error2}>
                              <CentredBox>
                                <Alert severity="error">{errorMsg2}</Alert>
                              </CentredBox>
                            </Collapse>
                          </CentredBox>
                        </Collapse>
                        {/* Group ticket buying menu */}
                        <Collapse in={groupTicketBuy}>
                          <GroupTickets reservedTickets={reservedTickets} setGroupTicketBuy={setGroupTicketBuy} setGroupLeaderTicket={setGroupLeaderTicket}/>
                        </Collapse>
                      </Collapse>
                    </Box>
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
                    <br/>
                    <Divider/>
                    <br/>
                    <Box sx={{backgroundColor: '#EEEEEE', display: 'flex', borderRadius: 2, p: 2}}>
                      {(selectedSeats.length === 0)
                        ? <CentredBox sx={{width: '100%'}}>
                            <ShoppingCartOutlinedIcon sx={{color: 'rgba(0, 0, 0, 0.6)'}}/>
                            <Typography sx={{color: 'rgba(0, 0, 0, 0.6)'}}>
                              Empty Cart
                            </Typography>
                          </CentredBox>
                        : <Box sx={{width: '100%'}}>
                            <Typography
                              sx={{textAlign: 'center', fontSize: 20, pb: 1}}
                            >
                              Order Summary
                            </Typography>
                            <Divider/>
                            <br/>
                            {!groupTicketBuy
                              ? <Box>
                                  <Grid container spacing={2} sx={{pl: 1, pr: 1}}>
                                    {selectedSeats.map((section, key) => {
                                      return (
                                        <Grid item key={key} sx={{width: '100%'}}>
                                          <Grid container spacing={2} sx={{pl: 1, pr: 1}}>
                                            <Grid item xs={9}>
                                              <Typography sx={{fontSize: 20}}>
                                                {section.quantity} x {section.section} - ${section.ticket_price}
                                              </Typography>
                                              {section.selectable
                                                ? <Box sx={{display: 'flex', gap: 1, flexWrap: 'wrap'}}>
                                                    {section.seatsSelected.map((seat, key) => {
                                                      if (key !== section.seatsSelected.length - 1) {
                                                        return (
                                                          <Typography key={key} sx={{color: 'rgba(0, 0, 0, 0.6)'}}>
                                                            {seat},
                                                          </Typography>
                                                        )
                                                      } else {
                                                        return (
                                                          <Typography key={key} sx={{color: 'rgba(0, 0, 0, 0.6)'}}>
                                                            {seat}
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
                                      <Typography sx={{fontSize: 20}}>
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
                              : <Box>
                                  <Grid container spacing={2} sx={{pl: 1, pr: 1}}>
                                    <Grid container spacing={2} sx={{pl: 1, pr: 1}}>
                                      <Grid item xs={9}>
                                        <Typography sx={{fontSize: 20}}>
                                          1 x {groupLeaderTicket.section} - ${groupLeaderTicket.price}
                                        </Typography>
                                        <Box sx={{display: 'flex', gap: 1, flexWrap: 'wrap'}}>
                                          <Typography sx={{color: 'rgba(0, 0, 0, 0.6)'}}>
                                            {groupLeaderTicket.section[0]}{groupLeaderTicket.seat_number}
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
                                      {(groupTicketBuy)
                                        ? <Typography sx={{fontSize: 25, textAlign: 'right'}}>
                                            ${groupLeaderTicket.price}
                                          </Typography>
                                        : <Typography sx={{fontSize: 25, textAlign: 'right'}}>
                                            ${orderTotal}
                                          </Typography>
                                      }
                                    </Grid>
                                  </Grid>
                                </Box>
                            }
                          </Box>
                      }
                    </Box>
                    <br/>
                  </Box>
                </Grid>
              </Grid>

            </EventForm>
        }
      </Box>
    </BackdropNoBG>
  )
}