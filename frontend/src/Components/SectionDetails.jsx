import React from "react"
import { FormControl, FormHelperText, Grid, Typography } from "@mui/material";
import { Box } from "@mui/system"
import { ContrastInput, ContrastInputWrapper } from "../Styles/InputStyles";
import { alpha } from '@mui/system';
import { CentredBox } from "../Styles/HelperStyles"
import { checkValidEmail, hasNumber, setFieldInState } from "../Helpers";


export default function SectionDetails ({section, getTicketDetails, handleTicketInput, handleSectionExpanded, reserve_id}) {
  return (
    <>
      <Box sx={{display: 'flex', justifyContent: 'center', boxShadow: 5, p: 1, borderRadius: 2, flexDirection: 'column'}}>
        <Grid container spacing={2}>
          <Grid item xs={8}>
            <Typography sx={{fontSize: 30}}>
              {section.quantity} x {section.section}
            </Typography>
          </Grid>
          <Grid item xs={4}>
          </Grid>
        </Grid>
        {section.reserved_seats.map((seat, key) => {
          return (
            <Ticket key={key} seatNum={seat.seat_number} reserve_id={seat.reserve_id} section={section} getTicketDetails={getTicketDetails} handleTicketInput={handleTicketInput}/>
          )
        })}
      </Box>
      <br/>
    </>
  )
}

function Ticket ({seatNum, section, getTicketDetails, reserve_id, handleTicketInput}) {

  // States
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

    // Check valid first name
    setFieldInState('value', e.target.value, firstName, setFirstName)
    handleTicketInput(reserve_id, 'first_name', e.target.value)
  }

  const handleLastNameChange = (e) => {
    // Clear error
    setFieldInState('error', false, lastName, setLastName)
    setFieldInState('errorMsg', '', lastName, setLastName)

    // Check valid last name
    setFieldInState('value', e.target.value, lastName, setLastName)
    handleTicketInput(reserve_id, 'last_name', e.target.value)
  }

  const handleEmailChange = (e) => {
    // Clear error
    setFieldInState('error', false, email, setEmail)
    setFieldInState('errorMsg', '', email, setEmail)

    // check valid email
    setFieldInState('value', e.target.value, email, setEmail)
    handleTicketInput(reserve_id, 'email', e.target.value)
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

    handleTicketInput(reserve_id, field, state.value)
  }

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
              <FormControl>
                <ContrastInputWrapper>
                  <ContrastInput
                    fullWidth
                    placeholder="First Name"
                    onChange={handleFirstNameChange}
                    value={firstName.value}
                    error={firstName.error}
                    onBlur={() => {handleOnBlur('first_name', firstName, setFirstName)}}
                  >
                  </ContrastInput>
                </ContrastInputWrapper>
                <FormHelperText>{firstName.errorMsg}</FormHelperText>
              </FormControl>
            </Grid>
            <Grid item xs={6}>
              <FormControl>
                <ContrastInputWrapper>
                  <ContrastInput
                    fullWidth
                    placeholder="Last Name"
                    onChange={handleLastNameChange}
                    value={lastName.value}
                    error={lastName.error}
                    onBlur={() => {handleOnBlur('last_name', lastName, setLastName)}}
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
                    onBlur={() => {handleOnBlur('email', email, setEmail)}}
                  >
                  </ContrastInput>
                </ContrastInputWrapper>
                <FormHelperText>{email.errorMsg}</FormHelperText>
              </FormControl>
            </Grid>
          </Grid>
        </Grid>
        <Grid item xs={2}>
        </Grid>
      </Grid>
    </Box>
  )
}