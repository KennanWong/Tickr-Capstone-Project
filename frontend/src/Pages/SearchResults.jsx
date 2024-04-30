import React from 'react'

import { Box } from '@mui/system'
import { useParams } from 'react-router-dom'
import { apiFetch, setFieldInState } from '../Helpers'
import { Grid, InputAdornment, TextField, Typography, Collapse, Divider, Tooltip, Link } from '@mui/material'
import SearchSharpIcon from '@mui/icons-material/SearchSharp';
import EventCardsPaper from '../Components/EventCardsPaper'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { useTheme } from '@mui/material/styles';
import dayjs from "dayjs";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider";
import { AdapterMoment } from "@mui/x-date-pickers/AdapterMoment";
import { DatePicker }  from '@mui/x-date-pickers/DatePicker';
import FormControl from "@mui/material/FormControl";


import ShadowInput from "../Components/ShadowInput";
import { ContrastInputWrapper, TextButton2, TickrSlider, TkrButton } from '../Styles/InputStyles'
import HeaderSearch from '../Components/HeaderSearch'
import TagsBar from '../Components/TagsBar'
import { BackdropNoBG_VH, CentredBox, ScrollableBox, ExpandMore } from '../Styles/HelperStyles'
import CategorySelector from '../Components/CategorySelector'

const ITEM_HEIGHT = 48;
const ITEM_PADDING_TOP = 8;

export default function SearchResults({}) {
  const params = useParams()
  var utc = require('dayjs/plugin/utc')
  dayjs.extend(utc)

  const [searchString, setSearchString] = React.useState('')
  const [resultsNum, setResultsNum] = React.useState(0)
  const [results, setResults] = React.useState([])
  const [moreResults, setMoreResults] = React.useState(false)
  const [showFilters, setShowFilters] = React.useState(false)
  const [searchOptions, setSearchOptions] = React.useState({
  })

  // Get Search options from params
  const getSearchOptions = () => {
    const searchParams = JSON.parse(atob(params.search_string))
    searchParams.location = null
    searchParams.max_distance = null
    searchParams.start_time = null
    searchParams.end_time = null
    setSearchOptions(searchParams)
    if (searchParams.text !== null) {
      setSearchString(searchParams.text)
    }
    setTags(searchParams.tags)
    setSelectCategories(searchParams.categories)
    
    if (searchParams.tags.length !== 0 || searchParams.categories.length !== 0) {
      setShowFilters(true)
    }
    // setFieldInState('value', searchParams.start_time, startDate, setStartDate)
    // setFieldInState('value', searchParams.end_time, endDate, setEndDate)
  } 

  React.useEffect(() => {
    getSearchOptions()
  }, [])

  // Search Filter states
  const [address, setAddress] = React.useState({
    value: '',
    error: false,
  })

  const [suburb, setSuburb] = React.useState({
    value: '',
    error: false,
  })

  const [postcode, setPostcode] = React.useState({
    value: '',
    error: false
  })

  const [state, setState] = React.useState({
    value: '',
    error: false
  })
  
  const [country, setCountry] = React.useState({
    value: '',
    error: false
  })

  const [distance, setDistance] = React.useState(100)
  const [tags, setTags] = React.useState([])
  const [errorStatus, setErrorStatus] = React.useState(false)

  const [startDate, setStartDate] = React.useState({
    value: null,
    error: false,
  });

  const [endDate, setEndDate] = React.useState({
    value: null,
    error: false,
    errorMsg: "",
  });

  const [selectCategories, setSelectCategories] = React.useState([])

  const [locationFilter, setLocationFilter] = React.useState(false)
  const [dateFilter, setDateFilter] = React.useState(false)
  const [categoriesFilter, setCategoriesFilter] = React.useState(false)
  const [tagsFilter, setTagsFilter] = React.useState(false)


  // Get search results given a page start
  const getSearchResults = async (pageStart) => {
    setResults([])
    const maxResults = 20
    try {
      const search_option_str = JSON.stringify(searchOptions)
      const body = {
        page_start: pageStart,
        max_results: maxResults,
        search_options: btoa(search_option_str)
      }

      const searchParams = new URLSearchParams(body)
      const response = await apiFetch('GET', `/api/event/search?${searchParams}`)
      if (pageStart === 0) {
        setResults([...response.event_ids])
        setResultsNum(response.event_ids.length)
        setMoreResults(!(response.event_ids.length === response.num_results))
      } else {
        const results_t = results.concat(response.event_ids)
        setResults([...results_t])
        setResultsNum(resultsNum + response.event_ids.length)
        setMoreResults(!((resultsNum + response.event_ids.length) === response.num_results))
      }
      
    } catch (e) {
      console.log(e)
    }
  }

  // Fetch Reviews on search string change
  React.useEffect(() => {
    getSearchResults(0)
  }, [searchOptions])

  // Effects to detect if filter is used
  React.useEffect(() => {
    if (address.value.length !== 0 && suburb.value.length !==  0 && postcode.value.length !== 0 && state.value.length !== 0 && country.value.length !== 0){
      setLocationFilter(true)
    } else {
      setLocationFilter(false)
    }
  }, [address, suburb, postcode, state, country])

  React.useEffect(() => {
    if (selectCategories.length > 0) {
      setCategoriesFilter(true)
    } else {
      setCategoriesFilter(false)
    }
  }, [selectCategories])

  React.useEffect(() => {
    if (tags.length > 0) {
      setTagsFilter(true)
    } else {
      setTagsFilter(false)
    }
  }, [tags])

  React.useState(() => {
    // Check date filter
    if (startDate.value !== null && endDate.value !== null) {
      setDateFilter(true)
    } else {
      setDateFilter(false)
    }
  }, [startDate.value, endDate.value])

  // Hanlde search change
  const handleStringChange = (e) => {
    setSearchString(e.target.value)
    setSearchOptions({...searchOptions, text: e.target.value})
  }

  // Hanlde request for mroe reviews
  const handleMoreEvents = () => {
    getSearchResults(resultsNum)
  }

  // Toggle show filter dropdown
  const togleShowFilters = () => {
    setShowFilters(!showFilters)
  }

  // Handle distance change
  const handleDistanceChange = (e, newValue) => {
    setDistance(newValue)
  }

  // Handle start date change
  const handleStartChange = (newValue) => {
    setFieldInState("value", newValue, startDate, setStartDate)

    if (newValue !== null && endDate.value !== null) {
      setDateFilter(true)
    } else {
      setDateFilter(false)
    }

    setFieldInState("error", false, startDate, setStartDate)
    if (endDate.value < startDate.value) {
      setFieldInState("error", true, endDate, setEndDate);
      setFieldInState(
        "errorMsg",
        "End date must be after start date",
        endDate,
        setEndDate
      );
    } else {
      setFieldInState("error", false, endDate, setEndDate);
      setFieldInState("errorMsg", "", endDate, setEndDate);
    }
  };

  // Handle end date change
  const handleEndChange = (newValue) => {
    setFieldInState("value", newValue, endDate, setEndDate);

    if (startDate.value !== null && newValue !== null) {
      setDateFilter(true)
    } else {
      setDateFilter(false)
    }

    setFieldInState("error", true, endDate, setEndDate);
    if (endDate.value <= startDate.value) {
      setFieldInState("error", true, endDate, setEndDate);
      setFieldInState(
        "errorMsg",
        "End date must be after start date",
        endDate,
        setEndDate
      );
    } else {
      setFieldInState("error", false, endDate, setEndDate);
      setFieldInState("errorMsg", "", endDate, setEndDate);
    }
  };

  // Handle disable start date
  function disableStartDate (date) {
    return (date <= startDate.value)
  }

  // Handle Categories change
  const handleCategoriesChange = (e) => {
    const {
      target: { value },
    } = e;
    setSelectCategories(
      // On autofill we get a stringified value.
      typeof value === 'string' ? value.split(',') : value,
    );
  }

  const handleApplyFilters = (e) => {
    // Check if address filter values
    var locationFilter_t = false
    if (address.value.length !== 0 && suburb.value.length !==  0 && postcode.value.length !== 0 && state.value.length !== 0 && country.value.length !== 0){
      setLocationFilter(true)
      locationFilter_t = true
    } else {
      setLocationFilter(false)
    }

    const streetAddress = address.value.split(' ')
    const locationBody = {
      street_no: (address.value.length !== 0) ? +streetAddress[0]: null,
      street_name: (address.value.length !== 0) ? streetAddress[1] + ' ' + streetAddress[2] : null,
      suburb: (suburb.value.length !==  0) ? suburb.value : null,
      postcode: (postcode.value.legnth !== 0) ? postcode.value : null,
      state: (state.value.length !== 0) ? state.value : null,
      country: (country.value.length !== 0) ? country.value : null
    }

    // Check date filter
    var dateFilter_t = false
    var start_date_t = null
    var end_date_t = null
    if (startDate.value !== null && endDate.value !== null) {
      setDateFilter(true)
      dateFilter_t = true
      start_date_t = dayjs(startDate.value).utc().format()
      end_date_t = dayjs(endDate.value).utc().format()
    } else {
      setDateFilter(false)
    }

    // Check categories filter
    var categoriesFilter_t = false
    if (selectCategories.length !== 0) {
      setCategoriesFilter(true)
      categoriesFilter_t = true
    } else {
      setCategoriesFilter(false)
    }

    // Check Tags filter
    var tagsFilter_t = false
    if (tags.length !== 0) {
      setTagsFilter(true)
      tagsFilter_t = true
    } else {
      setTagsFilter(false)
    }

    const search_filters = {
      location : locationFilter_t ? locationBody : null,
      max_distance: locationFilter_t ? parseFloat(distance).toFixed(2) : null,
      start_time: dateFilter_t ? start_date_t : null,
      end_time: dateFilter_t ? end_date_t : null,
      tags: tags,
      categories: selectCategories,
      text: searchString
    }

    setSearchOptions(search_filters)
  }

  const handleClearFilters = () => {
    // Clear location filters
    setFieldInState('value', '', address, setAddress)
    setFieldInState('value', '', suburb, setSuburb)
    setFieldInState('value', '', postcode, setPostcode)
    setFieldInState('value', '', state, setState)
    setFieldInState('value', '', country, setCountry)
    setDistance(100)
    setLocationFilter(false)

    // Clear date filters
    setFieldInState('value', null, startDate, setStartDate)
    setFieldInState('value', null, endDate, setEndDate)
    setDateFilter(false)

    // Clear categories filter
    setSelectCategories([])
    setCategoriesFilter(false)

    // Clear tags filter
    setTags([])
    setTagsFilter(false)

    setSearchOptions({
      text: searchString
    })
    
  }

  return (
    <Box sx={{overflow: 'hidden'}}>
      <BackdropNoBG_VH >
        <HeaderSearch />
        <ScrollableBox sx={{height: 'calc(100vh - 70px)'}}>
          <Box
            sx={{
              minHeight: 600,
              maxWidth: 2000,
              marginLeft: 'auto',
              marginRight: 'auto',
              width: '95%',
              backgroundColor: '#FFFFFF',
              marginTop: '50px',
              borderRadius: '15px',
              paddingBottom: 5,
            }}
          >
            <Box sx={{p: 5}}>
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <TextField 
                    fullWidth
                    InputProps={{
                      style: {fontSize: 40}, 
                      startAdornment: (
                        <InputAdornment position='start'>
                          <SearchSharpIcon sx={{ fontSize: 40 }}/>
                        </InputAdornment>
                      )
                    }}
                    placeholder="Search..." 
                    value={searchString} 
                    onChange={handleStringChange} 
                    variant="standard" 
                    color="secondary"
                  />
                </Grid>
                <Grid item xs={12}>
                  <Box sx={{display: 'flex', width: 'fit-content', justifyContent: 'flex-start'}}>
                    <Typography sx={{fontSize: 25, fontFamily: 'Segoe UI', color: showFilters ? '#333333' : '#CCCCCC'}}>
                      Filters
                    </Typography>
                    <ExpandMore
                      expand={showFilters}
                      onClick={togleShowFilters}
                    >
                      <ExpandMoreIcon sx={{color: showFilters ? '#333333' : '#CCCCCC'}}/>
                    </ExpandMore>
                  </Box>
                  <Collapse in={showFilters}>
                    <Box sx={{ p:4, backgroundColor: '#F6F6F6', borderRadius: 3}}>
                      <Grid container columns={24} spacing={1}>
                        <Grid item xs={9}>
                          <Typography sx={{fontFamily: 'Segoe UI', color: locationFilter ? '#333333' : '#AAAAAA', fontSize: 20, fontWeight: 'bold'}}>Location Filter</Typography>
                          <Grid container spacing={1}>
                            <Grid item xs={8}>
                              <ShadowInput 
                                state={address}
                                sx={{
                                  '.MuiOutlinedInput-notchedOutline': {
                                    borderColor: address.error ? "red" : "rgba(0,0,0,0)"
                                  },
                                }}
                                setState={setAddress}
                                defaultValue={address.value}
                                field='value'
                                placeholder="Street Address"
                                setError={setErrorStatus}
                              />
                            </Grid>
                            <Grid item xs={4}>
                              <ShadowInput 
                                state={suburb}
                                sx={{
                                  '.MuiOutlinedInput-notchedOutline': {
                                    borderColor: suburb.error ? "red" : "rgba(0,0,0,0)"
                                  },
                                }}
                                setState={setSuburb}
                                defaultValue={suburb.value}
                                field='value'
                                placeholder="Suburb"
                                setError={setErrorStatus}
                              />
                            </Grid>
                            <Grid item xs={3}>
                              <ShadowInput 
                                state={postcode} 
                                setState={setPostcode} 
                                sx={{
                                  '.MuiOutlinedInput-notchedOutline': {
                                    borderColor: postcode.error ? "red" : "rgba(0,0,0,0)"
                                  },
                                }}
                                defaultValue={postcode.value} 
                                field='value' 
                                placeholder="Postcode"
                                setError={setErrorStatus}
                              />
                            </Grid>
                            <Grid item xs={4}>
                            <ShadowInput 
                                state={state} 
                                setState={setState} 
                                sx={{
                                  '.MuiOutlinedInput-notchedOutline': {
                                    borderColor: state.error ? "red" : "rgba(0,0,0,0)"
                                  },
                                }}
                                defaultValue={state.value} 
                                field='value' 
                                placeholder="State"
                              />
                            </Grid>
                            <Grid item xs={5}>
                              <ShadowInput 
                                state={country} 
                                setState={setCountry} 
                                sx={{
                                  '.MuiOutlinedInput-notchedOutline': {
                                    borderColor: country.error ? "red" : "rgba(0,0,0,0)"
                                  },
                                }}
                                defaultValue={state.value} 
                                field='value' 
                                placeholder="Country"
                                setError={setErrorStatus}
                              />
                            </Grid>
                            <Grid item xs={12}>
                              <Box sx={{pt: 1, pl:2, pr: 2}}>
                                <Typography sx={{fontFamily: 'Segoe UI', color: locationFilter ? '#333333' : '#AAAAAA'}}>Max Distance From</Typography>
                                <TickrSlider
                                  min={1}
                                  max={100}
                                  value={distance}
                                  color='secondary'
                                  valueLabelDisplay="auto"
                                  marks={[{value: 1, label: '1km'}, {value: 100, label: '100km'}]}
                                  onChange={handleDistanceChange}/>
                              </Box>
                            </Grid>
                            <Grid item xs={12}>
                              <Box sx={{display: 'flex', alignItems: 'center', justifyContent: 'flex-end'}}>
                                <Tooltip title="Location data courtesy of OpenStreetMap">
                                  <Link underline='none' href="#" sx={{fontFamily: 'Segoe UI', color: '#AAAAAA'}} onClick={() => {window.open('https://openstreetmap.org/copyright')}}>
                                    ©OpenStreetMap
                                  </Link>
                                </Tooltip>
                              </Box>
                            </Grid>
                          </Grid>
                        </Grid>
                        <Divider orientation='vertical' flexItem sx={{pr: 1}}></Divider>
                        <Grid item xs={7}>
                          <Typography sx={{fontFamily: 'Segoe UI', color: dateFilter ? '#333333' : '#AAAAAA', fontSize: 20, fontWeight: 'bold'}}>Date Filter</Typography>
                          <Grid container spacing={1}>
                            <LocalizationProvider dateAdapter={AdapterMoment}>
                              <Grid item xs={12}>
                                <Box sx={{display: 'flex', gap: 1}}>
                                <FormControl fullWidth={false}>
                                  <Tooltip title="Start Date">
                                    <ContrastInputWrapper>
                                      <DatePicker
                                        sx={{".MuiOutlinedInput-notchedOutline" : {borderColor: 'rgba(0,0,0,0)'}}}
                                        value={startDate.value}
                                        onChange={handleStartChange}
                                        inputFormat="DD/MM/YYYY"
                                        disablePast = {true}
                                        renderInput={(params) => (
                                          <TextField
                                            {...params}
                                            inputProps={
                                              { 
                                                ...params.inputProps, 
                                                placeholder: "Start Date" 
                                              }
                                            }
                                          />
                                        )}
                                      />
                                    </ContrastInputWrapper>
                                  </Tooltip>
                                </FormControl>
                                <CentredBox>
                                  <Typography sx={{color: 'rgba(0, 0, 0, 0.6) ', fontFamily: 'Segoe UI'}}>to</Typography>
                                </CentredBox>
                                <FormControl fullWidth={false}>
                                  <Tooltip title="End Date">
                                    <ContrastInputWrapper>
                                      <DatePicker
                                        value={endDate.value}
                                        onChange={handleEndChange}
                                        inputFormat="DD/MM/YYYY"
                                        disablePast = {true}
                                        shouldDisableDate={disableStartDate}
                                        renderInput={(params) => (
                                          <TextField
                                            {...params}
                                            inputProps={
                                              { 
                                                ...params.inputProps, 
                                                placeholder: "End Date" 
                                              }
                                            }
                                          />
                                        )}
                                      />
                                    </ContrastInputWrapper>
                                  </Tooltip>
                                </FormControl>
                                </Box>
                              </Grid>
                            </LocalizationProvider>
                          </Grid>
                          <br/>
                          <FormControl fullWidth>
                            <Typography sx={{fontFamily: 'Segoe UI', color: categoriesFilter ? '#333333' : '#AAAAAA', fontSize: 20, fontWeight: 'bold'}}>Categories Filter</Typography>
                            <CategorySelector setSelectCategories={setSelectCategories} selectCategories={selectCategories} editable={true}/>
                          </FormControl>
                        </Grid>
                        <Divider orientation='vertical' flexItem sx={{pr: 1}}></Divider>
                        <Grid item xs={7}>
                          <Box>
                            <Typography sx={{fontFamily: 'Segoe UI', color: tagsFilter ? '#333333' : '#AAAAAA', fontSize: 20, fontWeight: 'bold'}}>Tags Filter</Typography>
                            <TagsBar tags={tags} setTags={setTags} editable={true}></TagsBar>
                          </Box>
                        </Grid>
                      </Grid>
                      <br/>
                      <Box sx={{display: 'flex', width: '100%', justifyContent: 'flex-end'}}>
                        <Box sx={{display:'flex', alignItems: 'center',  gap: 1, justifyContent: 'space-between'}}>
                          <TextButton2 sx={{fontSize: 20, width: 150, fontSize: 20, height: 30, textDecoration: 'underline', '&:hover': {textDecoration: 'underline'}}} onClick={handleClearFilters}>Clear Filters</TextButton2>
                          <TkrButton sx={{textTransform: 'none', width: 150, fontSize: 20, height: 30,}} onClick={handleApplyFilters}>Apply Filters</TkrButton> 
                        </Box>
                      </Box>
                    </Box>
                  </Collapse>
                </Grid>
                <Grid item xs={12}>
                  <EventCardsPaper events={results} moreEvents={moreResults} handleMoreEvents={handleMoreEvents}/>
                </Grid>
              </Grid>
            </Box>     
          </Box>
        </ScrollableBox>
      </BackdropNoBG_VH> 
    </Box>
  )
}