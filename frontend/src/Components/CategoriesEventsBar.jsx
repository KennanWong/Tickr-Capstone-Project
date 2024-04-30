import React from 'react'
import Box from '@mui/material/Box';
import { styled } from '@mui/system';
import { useNavigate } from "react-router-dom";
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import TabContext from '@mui/lab/TabContext';
import TabPanel from '@mui/lab/TabPanel';
import Button from '@mui/material/Button';
import { apiFetch, search } from '../Helpers';
import { useTheme } from '@mui/material/styles';
import { useRef } from 'react';
import EventsBar from './EventsBar';

const Section = styled(Box)({
  marginLeft: '1.5%',
  marginRight: '1.5%',
  height: '500px',
  fontSize: '50px',
  fontWeight: 'bold',
})

const SectionHeading = styled(Box)({
  display: 'flex',
  justifyContent: 'flex-start',
  gap: '10px',
  marginBottom: '10px'
})

export default function CategoriesEventsBar({}) {
  const ref = useRef(null)
  const navigate = useNavigate()
  const [upcomingValue, setUpcomingValue] = React.useState('0');
  const [categories, setCategories] = React.useState([])
  const upcomingChange = (event, newValue) => {
    setUpcomingValue(newValue);
  }; 

  const getCategories = async () => {
    try {
      const response = await apiFetch('GET', '/api/events/categories/list', null)
      setCategories(response.categories)
    } catch (e) {
      console.log(e)
    }
  }

  React.useEffect(() => {
    getCategories()
  }, [])

  const handleSeeAll = () => {
    search('categories', [categories[upcomingValue]], navigate)
  }

  return (
    <Section ref={ref} sx={{pt: 2, pb: 10}}>
      <TabContext value={upcomingValue}>
        <SectionHeading>
          <Box sx={{display: 'flex', alignItems: 'flex-end'}}>
            <Tabs
              onChange={upcomingChange}
              textColor="secondary"
              indicatorColor="secondary"
              scrollButtons
              value={upcomingValue}
              >
              {categories.map((category, key) => {
                return(
                  <Tab label={category} value={(key).toString()} key={key}/>
                )
              })}
            </Tabs>
          </Box>
          <Box
            sx={{
              width: 'auto',
              display: 'flex',
              alignItems: 'flex-end',
              justifyContent: 'flex-end',
              paddingBottom: '6px',
              flexGrow: '4'
            }}
          >
            <Button color='secondary' onClick={handleSeeAll}>
              see all
            </Button>
          </Box>
        </SectionHeading>
        {categories.map((category, key) => {
          return (
            <TabPanel key={key} value={(key).toString()} sx={{padding: 0}}>
              <EventsBar endpoint={'/api/events/category'} additionalParams={{category: category}} responseField={'event_ids'}/>
            </TabPanel>
          )
        })}
      </TabContext>
    </Section>
  )
}