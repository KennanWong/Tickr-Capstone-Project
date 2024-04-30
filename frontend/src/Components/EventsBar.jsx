import React from 'react'
import SwipeableViews from 'react-swipeable-views';
import MobileStepper from '@mui/material/MobileStepper';
import KeyboardArrowLeft from '@mui/icons-material/KeyboardArrowLeft';
import KeyboardArrowRight from '@mui/icons-material/KeyboardArrowRight';
import { useRef } from 'react';
import { Box } from '@mui/system'
import { useTheme } from '@mui/material/styles';
import Button from '@mui/material/Button';


import EventCardsBar from '../Components/EventCardsBar';
import EventCard from '../Components/EventCard';
import { apiFetch, attachFields } from '../Helpers';


const EVENT_CARD_WIDTH = 250

export default function EventsBar({endpoint, additionalParams={}, responseField, tokenReq=false}) {
  const theme = useTheme();
  const ref = useRef()

  const [activePage, setActivePage] = React.useState(0)
  const [eventGroups, setEventGroups] = React.useState([])
  const maxPages = eventGroups.length;
  const [windowWidth, setWindowWidth] = React.useState(0);
  const [cardsPerPage, setCardsPerPage] = React.useState(Math.floor((windowWidth/EVENT_CARD_WIDTH)))
  const [center, setCenter] = React.useState(false)

  // Set up listener for dynamically getting window width //
  const getWindowSize = () => {
    try {
      const elemWidth = ref.current.clientWidth
      const padding = 80
      const finalWidth = elemWidth-padding
      setWindowWidth(finalWidth)
      setCardsPerPage(Math.floor(finalWidth/EVENT_CARD_WIDTH))
    } catch (e) {
      
    }
    
  }
  React.useLayoutEffect(() => {
    getWindowSize()
  }, [])
  React.useEffect(() => {
    window.addEventListener("resize", getWindowSize)
  }, [])

  React.useEffect(() => {
    setCenter(false)
  }, [cardsPerPage])

  // ---------------------------------------------------- //

  // Handles for swipeable views interaction
  const handlePageChange = (step) => {
    setActivePage(step) 
  }

  const handlePageNext = () => {
    setActivePage((activePage) => activePage + 1)
  }

  const handlePageBack = () => {
    setActivePage((activePage) => activePage - 1)
  }


  // Function to get events
  const getEvents = async (pageStart, setEvents) => {
    setEvents([])

    const body_t = {
      max_results: cardsPerPage,
      page_start: pageStart,
    }

    // attach additional params]
    const body = attachFields(body_t, additionalParams)

    // call endpoint to get events
    try {
      const searchParams = new URLSearchParams(body)
      const response = await apiFetch('GET', `${endpoint}?${searchParams}`)
      
      // Set events
      const maxResults = response.num_results
      setEvents([...response[responseField]])
    } catch (e) {
      console.log(e)
    }
  }

  // Init fetch of events to know number of results to set up pages
  const initFetch = async () => {
    const body_t = {
      max_results: 1,
      page_start: 0,
    }
    // attach additional params
    const body = attachFields(body_t, additionalParams)

    // call endpoint to get events
    const searchParams = new URLSearchParams(body)
    const response = await apiFetch('GET', `${endpoint}?${searchParams}`)
  
    // Get max results to set up pages, check if cardsPerPage is 0, return
    if (cardsPerPage === 0) {
      return
    }
    const maxResults = response.num_results
    const numPages = Math.ceil(maxResults/cardsPerPage)
    const groups_t = []

    var i = 0
    while (i < numPages) {
      const group = {
        pageNum: i,
        pageStart: i*cardsPerPage,
      }
      groups_t.push(group)
      i += 1
    }

    if (maxResults === 0) {
      setEventGroups([{pageNum: 0, pageState: 0}])
    } else {
      setEventGroups([...groups_t])
    }

    setActivePage(0)
    
  }

  React.useEffect(() => {
    if (windowWidth !== 0) {
      initFetch()
    }
  }, [cardsPerPage])

  return (
    <Box sx={{display: 'flex', flexDirection: 'column'}}>
      <Box ref={ref}>
        <SwipeableViews
          axis={theme.direction === 'rtl' ? 'x-reverse' : 'x'}
          index={activePage}
          onChangeIndex={handlePageChange}
          enableMouseEvents
        >
          {eventGroups.map((event, key) => (
            <div key={key}>
              {Math.abs(activePage - key) <= 2 ? (
                <EventCardsPage setCenter={setCenter} center={center} pageStart={event.pageStart} pageNum={event.pageNum} activePage={activePage} getEvents={getEvents} cardsPerPage={cardsPerPage} tokenReq={tokenReq}/>
              ) : null}
            </div>
          ))}
        </SwipeableViews>
      </Box>
      <MobileStepper
        sx={{
          '.MuiMobileStepper-dotActive': { backgroundColor: '#AE759F' },
        }}
        steps={maxPages}
        position="static"
        activeStep={activePage}
        nextButton={
          <Button
            size="small"
            onClick={handlePageNext}
            disabled={activePage === maxPages - 1}
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
            onClick={handlePageBack}
            disabled={activePage === 0}
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
  )
}

function EventCardsPage({pageStart, getEvents, activePage, pageNum, cardsPerPage, center, setCenter, tokenReq=false}) {
  const [eventIds, setEventIds] = React.useState([])

  // Get event ids for page if active page is this page
  React.useEffect(() => {
    if (!center && (cardsPerPage === eventIds.length)) {
      setCenter(true)
    }
    if (activePage === pageNum) {
      if (pageStart !== undefined) {
        getEvents(pageStart, setEventIds)
      }
      
    }
  }, [activePage, cardsPerPage])


  // React.useEffect(() => {
  //   getEvents(pageStart, setEventIds)
  // }, [])

  return (
    <EventCardsBar center={(cardsPerPage === eventIds.length) || center} event_ids={eventIds} cardsPerBar={cardsPerPage} tokenReq={tokenReq}/>
  )
}