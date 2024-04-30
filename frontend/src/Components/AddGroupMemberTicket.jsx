import React from 'react'
import { Alert, alpha, Collapse, FormControl, FormHelperText, Grid, IconButton, Tooltip, Typography } from "@mui/material";
import { Box } from "@mui/system";
import { CentredBox } from "../Styles/HelperStyles";
import { ContrastInput, ContrastInputNoOutline, ContrastInputWrapper } from "../Styles/InputStyles";
import AddIcon from '@mui/icons-material/Add';
import { apiFetch, checkValidEmail, getToken } from "../Helpers";
import RemoveIcon from '@mui/icons-material/Remove';


export default function AddGroupMemberTicket({ticket, groupId, setTickets, tickets, index}) {

  const [email, setEmail] = React.useState('')
  const [error, setError] = React.useState(false)
  const [helperMsg, setHelperMsg] = React.useState('')

  const [reserveDetails, setReserveDetails] = React.useState({
    section: '',
    seat_number: ''
  })

  const getReserveDetails = async () => {
    try {
      const reserveDetailsBody = {
        reserve_id: ticket
      }
      const reserveSearchParams = new URLSearchParams(reserveDetailsBody)
      const reserveResponse = await apiFetch('GET', `/api/reserve/details?${reserveSearchParams}`, null)
      setReserveDetails(reserveResponse)
    } catch (e) {
      console.log(e)
    }
  }

  React.useEffect(() => {
    getReserveDetails()
  },[])

  // Handle Email Change
  const handleChange = (e) => {
    setEmail(e.target.value)
    setError(false)
    setHelperMsg('')
  }

  const handleInvite = async () => {
    if (!checkValidEmail(email)) {
      setError(true)
      setHelperMsg('Invalid email')
      return
    }
    // Check if user exists
    try {     
      const verifyUser = await apiFetch('GET', `/api/user/search?email=${email}`, null)
    } catch (error) {
      console.log(error)
      setError(true)
      setHelperMsg(error.reason)
      return
    }
    
    // Send invite
    try {
      const body = {
        auth_token: getToken(),
        group_id: groupId,
        email: email,
        reserve_id: ticket
      }

      const response = await apiFetch ('POST', '/api/group/invite', body)
      
      const tickets_t = tickets
      tickets_t.splice(index, 1)
      setTickets([...tickets_t])

    } catch (e) {
      console.log(e)
    }
  }

  // Handle if ticket is removed from gorup
  const handleRemoveTicket = async () => {
    try {
      const body = {
        auth_token: getToken(),
        reservations: [ticket]
      }

      const response = await apiFetch('DELETE', '/api/ticket/reserve/cancel', body)
      const tickets_t = tickets
      tickets_t.splice(index, 1)
      setTickets([...tickets_t])
    } catch (e) {
      console.log(e)
    }
  }

  return (
    <Box sx={{width: '100%', pt: 2, pb: 2}}>
      <Grid container spacing={2}>
        <Grid item xs={1}></Grid>
        <Grid item xs={3}>
          <CentredBox sx={{backgroundColor: alpha('#6A7B8A', 0.3), height: '100%', width: '100%', borderRadius: 3}}>
            <Typography>
              {reserveDetails.section[0]}{reserveDetails.seat_number}
            </Typography>
          </CentredBox>
        </Grid>
        <Grid item xs={7}>
          <Box>
            <ContrastInputWrapper>
              <ContrastInputNoOutline
                fullWidth
                endAdornment={
                  <Tooltip title="Send Invite">
                    <span>
                      <IconButton
                        disabled={email.length <= 0}
                        onClick={handleInvite}
                      >
                        <AddIcon/>
                      </IconButton>
                    </span>
                  </Tooltip>
                }
                placeholder="Email"
                error={error}
                onChange={handleChange}
                value={email}
              >
              </ContrastInputNoOutline>
            </ContrastInputWrapper>
          </Box>
        </Grid>
        <Grid item xs={1}>
          <CentredBox sx={{height: '100%'}}>
            <Tooltip title={'Remove ticket from group'}>
              <IconButton onClick={handleRemoveTicket}>
                <RemoveIcon/>
              </IconButton>
            </Tooltip>
          </CentredBox>
        </Grid>
      </Grid>
      <CentredBox sx={{pt: 1}}>
        <Collapse in={error}>
          <Alert severity="error">
            {helperMsg}
          </Alert>
        </Collapse>
      </CentredBox>
    </Box>
  )
}