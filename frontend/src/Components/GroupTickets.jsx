import { Collapse, FormControl, FormHelperText, Divider, Grid, IconButton, Tooltip, Typography, Button, styled, alpha, Alert } from "@mui/material";
import { Box } from "@mui/system";
import React from "react";
import { CentredBox } from "../Styles/HelperStyles";
import { TextButton, TextButton2, TicketOption, TkrButton } from "../Styles/InputStyles";
import GroupTicket from "./GroupTicket";
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import Stepper from '@mui/material/Stepper';
import Step from '@mui/material/Step';
import StepLabel from '@mui/material/StepLabel';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { checkValidEmail, setFieldInState, hasNumber, getToken, apiFetch } from "../Helpers";
import { ContrastInput, ContrastInputWrapper } from "../Styles/InputStyles";
import { useParams } from "react-router-dom";

const steps = ['Select assigned ticket', 'Invite group members']

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


export default function GroupTickets({reservedTickets, setGroupTicketBuy, createGroup, setGroupLeaderTicket}) {
  const params = useParams()

  // States
  const [info, setInfo] = React.useState(true)
  const [activeStep, setActiveStep] = React.useState(0);
  const [showSelect, setShowSelect] = React.useState(true)
  const [showInvite, setShowInvite] = React.useState(false)
  const [showInviteInfo, setShowInviteInfo] = React.useState(false)
  const [selectTicket, setSelectTicket] = React.useState('')
  const [invites, setInvites] = React.useState([])
  const [groupId, setGroupId] = React.useState('')
  const [groupCreated, setGroupCreated] = React.useState(false)

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

  // handle onblur event, setState in parent element, to avoid unnecesary re-renders
  const handleOnBlur = (field, state, setState) => {
    // check valid input
    if (state === email) {
      if (!checkValidEmail(state.value)){
        setFieldInState('error', true, state, setState)
        setFieldInState('errorMsg', 'Invalid email.', state, setState)
        return
      }
    } else {
      if (state.value.length <= 0 || hasNumber(state.value)) {
        setFieldInState('error', true, state, setState)
        setFieldInState('errorMsg', `Invalid ${state.label}.`, state, setState)
        return
      }
    }
    
  }
  const cancelTicketBuy = () => {
    // Reset field
    setGroupCreated(false)
    setSelectTicket('')
    setFieldInState('value', '', firstName, setFirstName)
    setFieldInState('value', '', lastName, setLastName)
    setFieldInState('value', '', email, setEmail)
    setShowSelect(true)
    setShowInvite(false)
    setGroupId('')
    setInfo(true)
    setGroupTicketBuy(false)
  }

  const handleShowInfo = () => {
    setInfo(!info)
  }

  const handleShowSelect = () => {
    setShowSelect(!showSelect)
  }

  const handleShowInvite = () => {
    setShowInvite(!showInvite)
  }

  // Handle group leaders selected ticket
  const handleSelectTicket = (reserve_id) => {
    setSelectTicket(reserve_id)

    // Find ticket and set leader ticket to display in cart
    reservedTickets.forEach(function(reserve) {
      if (reserve.reserve_id === reserve_id) {
        setGroupLeaderTicket(reserve)
      }
    })
  }

  const handleCreateGroup = async () => {
    // Verify input details
    if (!checkValidEmail(email.value)){
      setFieldInState('error', true, email, setEmail)
      setFieldInState('errorMsg', 'Invalid email.', email, setEmail)
      return
    }

    if (firstName.value.length <= 0 || hasNumber(firstName.value)) {
      setFieldInState('error', true, firstName, setFirstName)
      setFieldInState('errorMsg', `Invalid first name.`, firstName, setFirstName)
      return
    }

    if (lastName.value.length <= 0 || hasNumber(lastName.value)) {
      setFieldInState('error', true, lastName, setLastName)
      setFieldInState('errorMsg', `Invalid last name.`, lastName, setLastName)
      return
    }

    try {

      const reserveIds = []
      reservedTickets.forEach(function (reserve) {
        reserveIds.push(reserve.reserve_id)
      })
      
      const body = {
        auth_token: getToken(),
        reserved_ids: reserveIds,
        host_reserve_id: selectTicket,
      }

      const response = await apiFetch('POST', '/api/group/create', body)
      setGroupId(response.group_id)
      
    } catch (e) {
      console.log(e)
    }
    setShowInvite(true)
    setShowInviteInfo(true)
    setInvites([])
    setInfo(false)
    setGroupCreated(true)
    setShowSelect(false)
  }

  React.useEffect(() => {
    setShowInviteInfo(false)
  }, [invites])

  // Group leader purchases their ticket
  const handleCheckout = async () => {
    const ticketDetails = [{
      first_name: firstName.value,
      last_name: lastName.value,
      email: email.value,
      request_id: selectTicket
    }]

    const body = {
      auth_token: getToken(),
      ticket_details: ticketDetails,
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

  return (
    <CentredBox sx={{flexDirection: 'column', ml: 5, mr: 5}}>
      <Box sx={{width: '100%'}}>
        <Box sx={{display: 'flex', justifyContent: 'space-between'}}>
          <Typography
            sx={{
              fontFamily: "Segoe UI",
              fontSize: 25,
              textAlign: 'start',
            }}
          >
            Group Ticket Buying
          </Typography>
          <Tooltip title="More about group buying">
            <IconButton onClick={handleShowInfo}>
              <InfoOutlinedIcon/>
            </IconButton>
          </Tooltip>
        </Box>
        <Collapse in={info}>
          <Box sx={{p: 3, backgroundColor: '#DDDDDD', borderRadius: 5, mb: 1}}>
            <Typography
              sx={{
                fontFamily: "Segoe UI",
                fontSize: 17,
              }}
            >
              Group ticket buying allows for one person to reserve tickets for a group
              The group leader is able to send invites to other users of Tickr to purchase
              their delegated seat.
            </Typography>
          </Box>
        </Collapse>
      </Box>
      <Box sx={{width: '100%'}}>
        {/* Select gorup leaders ticket */}
        <Box sx={{display: 'flex', justifyContent: 'space-between'}}>
          <Typography
            sx={{
              fontFamily: "Segoe UI",
              fontSize: 20,
              textAlign: 'start',
              width: '100%',
              fontWeight: 'bold',
              color: (groupCreated) ? '#EEEEEE' : '#333333'
            }}
          >
            1. Select your ticket
          </Typography> 
        </Box>
        <Collapse in={showSelect}>
          <CentredBox sx={{gap: 5, flexWrap: 'wrap'}}>
            {reservedTickets.map((reserve, key) => {
              return (
                <TicketOption 
                  sx={{
                    widht: 70,
                    height: 70,
                    backgroundColor: (reserve.reserve_id === selectTicket) ? "#AE759F" : alpha('#6A7B8A', 0.3),
                    color: ((reserve.reserve_id === selectTicket) ? '#FFFFFF' : '#444444')
                  }} 
                  key={key} 
                  onClick={() => {handleSelectTicket(reserve.reserve_id)}}
                >
                  {reserve.section[0]}{reserve.seat_number}
                </TicketOption>
              )
            })}
          </CentredBox>
          <br/> 
          <CentredBox sx={{ml: 15, mr: 15}}>
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
                  <TkrButton disabled={(selectTicket === '')} sx={{textTransform: 'none', width: 90, height: 40}} onClick={handleCreateGroup}>Proceed</TkrButton>
                  <Collapse sx={{pt: 1}} in={(firstName.error || lastName.error || email.error)}>
                    <Alert severity="error">{firstName.error ? firstName.errorMsg : ''}{lastName.error ? lastName.errorMsg : ''}{email.error ? email.errorMsg : ''}</Alert>
                  </Collapse>
                </CentredBox>
              </Grid>
            </Grid>
          </CentredBox>
        </Collapse>
      </Box>
      <Box sx={{width: '100%'}}>
        <Box sx={{display: 'flex', justifyContent: 'space-between'}}>
          <Typography
            sx={{
              fontFamily: "Segoe UI",
              fontSize: 20,
              textAlign: 'start',
              width: '100%',
              fontWeight: 'bold',
              color: (!groupCreated) ? '#EEEEEE' : '#333333'
            }}
          >
            2. Invite Group members
          </Typography>
          <ExpandMore
            expand={showInvite}
            onClick={handleShowInvite}
            disabled={(!groupCreated)}
          >
            <ExpandMoreIcon/>
          </ExpandMore>
        </Box>
        <Collapse in={showInvite}>
          <Box sx={{p: 3, backgroundColor: '#DDDDDD', borderRadius: 5, mb: 1}}>
            <Typography
              sx={{
                fontFamily: "Segoe UI",
                fontSize: 17,
              }}
            >
              Invite other members of Tickr. This can also be done after the ticket has been purchased.
            </Typography>
          </Box>
        </Collapse>
        <Collapse in={showInvite}>
          <CentredBox sx={{flexDirection: 'column'}}>
            {reservedTickets.map((ticket, key) => {
              if (ticket.reserve_id !== selectTicket) {
                return (
                  <GroupTicket key={key} ticket={ticket} setInvites={setInvites} invites={invites} groupId={groupId}/>
                )
              } 
            })}
          </CentredBox>
        </Collapse>
      </Box>
      <Grid container spacing={2}>
        <Grid item xs={1}>
        </Grid>
        <Grid item xs={11}>
          <Box sx={{display: 'flex', justifyContent: 'flex-end', alignItems: 'center'}}>
            <TextButton2 onClick={cancelTicketBuy}>
              Cancel group ticket
            </TextButton2>
          </Box>
        </Grid>
      </Grid>
      {showInvite
        ? <CentredBox>
            <TkrButton onClick={handleCheckout}>Checkout</TkrButton>
          </CentredBox>
        : <></>

      }
    </CentredBox>
  )
}