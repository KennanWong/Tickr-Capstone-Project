import React from "react"
import { BackdropNoBG, CentredBox } from "../Styles/HelperStyles"
import Header from "../Components/Header"
import { getEventData, getTicketIds } from "../Helpers"
import { useParams, useNavigate } from "react-router-dom"
import { Box } from "@mui/system"
import TicketCard from "../Components/TicketCard"
import { useTheme } from '@mui/material/styles';
import MobileStepper from '@mui/material/MobileStepper';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import KeyboardArrowLeft from '@mui/icons-material/KeyboardArrowLeft';
import KeyboardArrowRight from '@mui/icons-material/KeyboardArrowRight';
import SwipeableViews from 'react-swipeable-views';
import { Divider, Grid, IconButton, Tooltip } from "@mui/material"
import ArrowBackIcon from "@mui/icons-material/ArrowBack"

export default function ViewTickets({}) {
  const params = useParams()
  const navigate = useNavigate()
  const theme = useTheme();

  const [event, setEvent] = React.useState(null)

  const [ticketIds, setTicketIds] = React.useState([])

  const [activeStep, setActiveStep] = React.useState(0);
  const maxSteps = ticketIds.length;

  const handleNext = () => {
    setActiveStep((prevActiveStep) => prevActiveStep + 1);
  };

  const handleBack = () => {
    setActiveStep((prevActiveStep) => prevActiveStep - 1);
  };

  const handleStepChange = (step) => {
    setActiveStep(step);
  };

  // Initial fetch of event data
  React.useEffect(() => {
    if (ticketIds.length === 0) {
      getEventData(params.event_id, setEvent)
    }
  }, [])

  // Initial fetch of tickets
  React.useEffect(() => {
    if (event !== null) {
      // Fetch Ticket ids
      getTicketIds(params.event_id, setTicketIds)
    } 
  }, [event])

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
            <Box sx={{p: 3, pl: 10}}>
              <Tooltip title="Back to event page">
                <IconButton onClick={()=>{navigate(`/view_event/${params.event_id}`)}}>
                  <ArrowBackIcon/>
                </IconButton>
              </Tooltip>
            </Box>
          </Grid>
          <Grid item xs={10}>
            <CentredBox sx={{display: 'flex', flexDirection: 'column', height: '100%', alignItems: 'center'}}>
              <br/>
              <Typography component={'span'} sx={{fontSize: 35, fontWeight: 'bold'}}>
                My Tickets
                <Divider/>
              </Typography>
              <Box sx={{display: 'flex', flexDirection: 'column', width: '60%'}}>
                <Box>
                  <SwipeableViews
                    axis={theme.direction === 'rtl' ? 'x-reverse' : 'x'}
                    index={activeStep}
                    onChangeIndex={handleStepChange}
                    enableMouseEvents
                  >
                    {ticketIds.map((ticketId, key) => (
                      <div key={key}>
                        {Math.abs(activeStep - key) <= 2 ? (
                          <TicketCard event={event} ticket_id={ticketId} ticketOwner={true}/>
                        ) : null}
                      </div>
                    ))}
                  </SwipeableViews>
                </Box>
              </Box>
              <Box width={'60%'}>
                <MobileStepper
                  sx={{
                    '.MuiMobileStepper-dotActive': { backgroundColor: '#AE759F' },
                  }}
                  steps={maxSteps}
                  position="static"
                  activeStep={activeStep}
                  nextButton={
                    <Button
                      size="small"
                      onClick={handleNext}
                      disabled={activeStep === maxSteps - 1}
                      sx={{color: '#AE759F'}}
                    >
                      Next
                      {theme.direction === 'rtl' ? (
                        <KeyboardArrowLeft />
                      ) : (
                        <KeyboardArrowRight />
                      )}
                    </Button>
                  }
                  backButton={
                    <Button
                      size="small"
                      onClick={handleBack}
                      disabled={activeStep === 0}
                      sx={{color: '#AE759F'}}
                    >
                      {theme.direction === 'rtl' ? (
                        <KeyboardArrowRight />
                      ) : (
                        <KeyboardArrowLeft />
                      )}
                      Back
                    </Button>
                  }
                />
              </Box>
            </CentredBox>
          </Grid>
          <Grid item xs={1}></Grid>
        </Grid>
        
      </Box>
      
    </BackdropNoBG>
  )
}
