import React from 'react'
import Box from '@mui/material/Box';
import Header from '../Components/Header'
import { Backdrop, BackdropNoBG, CentredBox, ContentBox } from '../Styles/HelperStyles';
import { styled } from '@mui/system';
import { Link } from "react-router-dom";
import Grid from '@mui/material/Grid';
import { Card, Container, Divider, Typography } from '@mui/material';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import TabContext from '@mui/lab/TabContext';
import TabList from '@mui/lab/TabList';
import TabPanel from '@mui/lab/TabPanel';
import EventCard from '../Components/EventCard';
import Button from '@mui/material/Button';
import { apiFetch } from '../Helpers';

const CardsBar = styled(Box)({
  backgroundColor: '#F6F6F6',
  height: '400px',
  borderRadius: '10px',
  padding: '15px',
  fontSize: '15px',
  fontWeight: 'light',
  display: 'flex',
  justifyContent: 'flex-start',
  gap: 20,
  flexWrap: 'nowrap'
})

const FillerCard = styled(Box)({
  width: '250px',
  height: '400px',
})

export default function EventCardsBar({event_ids = [], filterKeys=[], filterValues=[], center=false, cardsPerBar, tokenReq=false}) {
  const [eventIds, setEventIds] = React.useState([...event_ids])

  React.useEffect(() => {
    setEventIds([...event_ids])
    const fillerNum = cardsPerBar - event_ids.length
    if (fillerNum !== 0 && center) {
      const filler_t = Array.from(Array(fillerNum).keys())
      setFillerCard([...filler_t])
    } else {
      setFillerCard([])
    }
    
    
  }, [event_ids, center])

  const [fillerCard, setFillerCard] = React.useState([])



  return (
    <Box sx={{display: 'flex', width: '100%', backgroundColor: '#F6F6F6', borderRadius: 3, justifyContent: (center || event_ids.length === 0)  ? 'center' : 'flex-start'}}>
      {(event_ids.length > 0)
        ? <CardsBar>
            {eventIds.map((value, key) => {
              return (
                <EventCard key={key} event_id={value} tokenReq={tokenReq}/>
              )
            })}
            {fillerCard.map((value, key) => {
              return (
                <FillerCard key={key}/>
              )
            })}
          </CardsBar>
        : <CardsBar sx={{alignItems: 'center'}}>
            <Box sx={{width: '100%'}}>
              <Typography sx={{fontSize: 45, color: '#CCCCCC', textAlign: 'center'}}>
                No Events
              </Typography>
            </Box>
            
          </CardsBar>
      }
      
    </Box>
  )
}